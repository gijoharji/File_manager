package com.filemanager.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.MediaStore
import com.filemanager.app.data.CategoryData
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.FileItem
import com.filemanager.app.data.SourceFolderData
import com.filemanager.app.data.StorageEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    private val groupingRoots = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        File(Environment.getExternalStorageDirectory(), "WhatsApp/Media"),
        File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp")
    ).filter { it.exists() || it.parentFile?.exists() == true }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (kotlin.math.ln(bytes.toDouble()) / kotlin.math.ln(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        val divisor = Math.pow(1024.0, exp.toDouble())
        return String.format("%.2f %sB", bytes / divisor, pre.toString())
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    suspend fun scanFiles(context: Context): Map<FileCategory, CategoryData> = withContext(Dispatchers.IO) {
        val storageRoot = Environment.getExternalStorageDirectory()
        val categoryMap = mutableMapOf<FileCategory, MutableMap<String, MutableList<FileItem>>>()

        FileCategory.values().forEach { category ->
            categoryMap[category] = mutableMapOf()
        }

        // Scan storage
        scanDirectoryRecursive(storageRoot, categoryMap)

        // Some document locations (like Android/media) may be visible only via MediaStore APIs.
        // Augment our scan with anything MediaStore exposes for the Documents bucket so the
        // category reflects all accessible files for the user.
        collectDocumentsFromMediaStore(context, categoryMap)

        // Build result
        categoryMap.mapValues { (category, sourceMap) ->
            val totalFiles = sourceMap.values.flatten()
            val totalSize = totalFiles.sumOf { it.size }

            val sourceEntries = sourceMap.map { (path, files) ->
                path to SourceFolderData(
                    path = path,
                    name = getFolderDisplayName(path),
                    itemCount = files.size,
                    totalSize = files.sumOf { it.size },
                    files = files.sortedByDescending { it.dateModified }
                )
            }.sortedWith(compareBy({ it.second.name.lowercase(Locale.getDefault()) }, { it.first }))

            val sortedSources = LinkedHashMap<String, SourceFolderData>().apply {
                sourceEntries.forEach { (path, data) -> put(path, data) }
            }

            CategoryData(
                category = category,
                itemCount = totalFiles.size,
                totalSize = totalSize,
                sources = sortedSources
            )
        }
    }

    private fun scanDirectoryRecursive(
        directory: File,
        categoryMap: MutableMap<FileCategory, MutableMap<String, MutableList<FileItem>>>
    ) {
        try {
            if (!directory.exists() || !directory.isDirectory || !directory.canRead()) {
                return
            }

            val files = directory.listFiles() ?: return

            for (file in files) {
                if (file.isHidden) continue

                if (file.isFile) {
                    val category = classifyFile(file)
                    if (category != null) {
                        val sourcePath = getSourcePath(file)
                        categoryMap[category]?.getOrPut(sourcePath) { mutableListOf() }?.add(
                            FileItem(
                                path = file.absolutePath,
                                name = file.name,
                                size = file.length(),
                                dateModified = file.lastModified(),
                                category = category
                            )
                        )
                    }
                } else if (file.isDirectory) {
                    // Skip hidden and system cache directories
                    if (file.name.startsWith(".") ||
                        file.name.equals("Lost.Dir", ignoreCase = true)) {
                        continue
                    }

                    // Avoid scanning restricted Android directories while keeping Android/media
                    val parentName = file.parentFile?.name?.lowercase(Locale.getDefault())
                    val nameLower = file.name.lowercase(Locale.getDefault())
                    val isAndroidChild = parentName == "android"
                    if (isAndroidChild && (nameLower == "data" || nameLower == "obb")) {
                        continue
                    }

                    scanDirectoryRecursive(file, categoryMap)
                }
            }
        } catch (e: Exception) {
            // Skip directories we can't access
        }
    }

    @SuppressLint("Range")
    private fun collectDocumentsFromMediaStore(
        context: Context,
        categoryMap: MutableMap<FileCategory, MutableMap<String, MutableList<FileItem>>>
    ) {
        val documentsBucket = categoryMap[FileCategory.DOCUMENTS] ?: return
        val knownPaths = documentsBucket.values
            .flatMapTo(mutableSetOf()) { source -> source.map(FileItem::path) }

        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())

        val cursor = try {
            resolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                projection,
                selection,
                selectionArgs,
                null
            )
        } catch (e: Exception) {
            null
        } ?: return

        cursor.use { c ->
            val storageRoot = Environment.getExternalStorageDirectory()
            val relativeIndex = c.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
            val dataIndex = c.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val nameIndex = c.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeIndex = c.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val dateIndex = c.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (c.moveToNext()) {
                val displayName = if (nameIndex != -1) c.getString(nameIndex) else null
                if (displayName.isNullOrBlank()) continue

                val absolutePath = when {
                    dataIndex != -1 -> c.getString(dataIndex)
                    relativeIndex != -1 -> {
                        val relative = c.getString(relativeIndex) ?: continue
                        File(storageRoot, relative).resolve(displayName).absolutePath
                    }
                    else -> null
                } ?: continue

                val file = File(absolutePath)
                val category = classifyFile(file) ?: continue
                if (category != FileCategory.DOCUMENTS) continue

                if (!knownPaths.add(absolutePath)) continue

                val size = if (sizeIndex != -1) c.getLong(sizeIndex) else file.length()
                val modified = if (dateIndex != -1) c.getLong(dateIndex) * 1000 else file.lastModified()
                val sourcePath = getSourcePath(file)

                documentsBucket.getOrPut(sourcePath) { mutableListOf() }.add(
                    FileItem(
                        path = file.absolutePath,
                        name = file.name,
                        size = size,
                        dateModified = modified,
                        category = FileCategory.DOCUMENTS
                    )
                )
            }
        }
    }

    private fun classifyFile(file: File): FileCategory? {
        val category = FileCategory.fromFile(file)
        if (category != null) {
            return category
        }

        return null
    }
    private fun getSourcePath(file: File): String {
        val absolutePath = file.absolutePath
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath
        
        // Check common sources
        groupingRoots.forEach { root ->
            val rootPath = root.absolutePath
            if (absolutePath.startsWith(rootPath)) {
                val parent = file.parentFile ?: return rootPath
                if (parent.absolutePath == rootPath) {
                    return rootPath
                }

                val relativeParent = parent.absolutePath
                    .removePrefix(rootPath)
                    .trim(File.separatorChar)
                val firstSegment = relativeParent.substringBefore(File.separatorChar, "")

                return if (firstSegment.isNotEmpty()) {
                    File(rootPath, firstSegment).absolutePath
                } else {
                    parent.absolutePath
                }
            }
        }

        // Otherwise, use parent directory as source
        val parent = file.parentFile ?: return storageRoot
        return parent.absolutePath
    }

    private fun getFolderDisplayName(path: String): String {
        val file = File(path)
        val name = file.name

        if (name.isNotBlank() && name !in setOf("storage", "emulated", "0")) {
            return name
        }

        val parent = file.parentFile
        val parentName = parent?.name
        if (!parentName.isNullOrBlank() && parentName !in setOf("storage", "emulated", "0")) {
            return parentName
        }

        return "Other"
    }

    fun getStorageVolumes(context: Context): List<StorageVolume> {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            storageManager.storageVolumes
        } else {
            emptyList()
        }
    }

    fun getStorageInfo(path: String): Pair<Long, Long> {
        return try {
            val stat = StatFs(path)
            val blockSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.blockSizeLong
            } else {
                stat.blockSize.toLong()
            }
            val totalBlocks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.blockCountLong
            } else {
                stat.blockCount.toLong()
            }
            val availableBlocks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBlocksLong
            } else {
                stat.availableBlocks.toLong()
            }
            
            val totalSpace = totalBlocks * blockSize
            val usedSpace = (totalBlocks - availableBlocks) * blockSize
            Pair(totalSpace, usedSpace)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }

    fun getDownloadsInfo(): Pair<Int, Long> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                val files = downloadsDir.listFiles() ?: emptyArray()
                val count = files.count { it.isFile }
                val totalSize = files.filter { it.isFile }.sumOf { it.length() }
                Pair(count, totalSize)
            } else {
                Pair(0, 0L)
            }
        } catch (e: Exception) {
            Pair(0, 0L)
        }
    }

    fun deleteFiles(files: List<FileItem>): Boolean {
        return files.all { fileItem ->
            try {
                File(fileItem.path).delete()
            } catch (e: Exception) {
                false
            }
        }
    }

    fun copyFiles(files: List<FileItem>, destination: File): Boolean {
        return files.all { fileItem ->
            try {
                val sourceFile = File(fileItem.path)
                val destFile = File(destination, fileItem.name)
                
                // Handle duplicates
                var finalDest = destFile
                var counter = 1
                while (finalDest.exists()) {
                    val nameWithoutExt = fileItem.name.substringBeforeLast(".")
                    val ext = fileItem.name.substringAfterLast(".", "")
                    finalDest = File(destination, "$nameWithoutExt ($counter).$ext")
                    counter++
                }
                
                sourceFile.copyTo(finalDest, overwrite = false)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun moveFiles(files: List<FileItem>, destination: File): Boolean {
        return files.all { fileItem ->
            try {
                val sourceFile = File(fileItem.path)
                val destFile = File(destination, fileItem.name)

                // Handle duplicates
                var finalDest = destFile
                var counter = 1
                while (finalDest.exists()) {
                    val nameWithoutExt = fileItem.name.substringBeforeLast(".")
                    val ext = fileItem.name.substringAfterLast(".", "")
                    finalDest = File(destination, "$nameWithoutExt ($counter).$ext")
                    counter++
                }

                sourceFile.renameTo(finalDest)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun renameFile(file: FileItem, newName: String): Boolean {
        return try {
            val sourceFile = File(file.path)
            val parentDirectory = sourceFile.parentFile ?: return false

            val trimmedName = newName.trim()
            if (trimmedName.isEmpty()) return false

            val desiredName = if (trimmedName.contains('.')) {
                trimmedName
            } else {
                val originalExtension = file.name.substringAfterLast('.', "")
                if (originalExtension.isNotEmpty()) {
                    "$trimmedName.$originalExtension"
                } else {
                    trimmedName
                }
            }

            val targetFile = File(parentDirectory, desiredName)
            if (targetFile.exists() && targetFile.absolutePath != sourceFile.absolutePath) {
                return false
            }

            sourceFile.renameTo(targetFile)
        } catch (e: Exception) {
            false
        }
    }

    fun listDirectoryEntries(context: Context, path: String): List<StorageEntry> {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory || !directory.canRead()) {
            return emptyList()
        }

        val children = try {
            directory.listFiles()
        } catch (e: Exception) {
            null
        }?.filterNot { it.isHidden } ?: return emptyList()

        return children.map { file ->
            val (itemCount, size) = if (file.isDirectory) {
                getDirectoryMetrics(context, file)
            } else {
                Pair(0, file.length())
            }

            StorageEntry(
                path = file.absolutePath,
                name = file.name.ifBlank { file.absolutePath },
                isDirectory = file.isDirectory,
                size = size,
                itemCount = itemCount,
                lastModified = file.lastModified()
            )
        }.sortedWith(
            compareByDescending<StorageEntry> { it.isDirectory }
                .thenBy { it.name.lowercase(Locale.getDefault()) }
        )
    }

    private fun getDirectoryMetrics(context: Context, directory: File): Pair<Int, Long> {
        val visibleChildren = try {
            directory.listFiles()?.filterNot { it.isHidden }
        } catch (e: Exception) {
            null
        }

        if (visibleChildren != null) {
            val totalSize = calculateDirectorySize(directory)
            return Pair(visibleChildren.size, totalSize)
        }

        val fallback = queryDirectoryViaMediaStore(context, directory)
        return fallback ?: Pair(0, 0L)
    }

    private fun calculateDirectorySize(directory: File): Long {
        var totalSize = 0L
        val stack: ArrayDeque<File> = ArrayDeque()
        val visited = mutableSetOf<String>()
        stack.add(directory)

        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            val canonicalPath = try {
                current.canonicalPath
            } catch (e: Exception) {
                current.absolutePath
            }

            if (!visited.add(canonicalPath)) {
                continue
            }

            val files = try {
                current.listFiles()
            } catch (e: Exception) {
                null
            } ?: continue

            for (child in files) {
                if (child.isHidden) continue

                try {
                    if (child.isFile) {
                        totalSize += child.length()
                    } else if (child.isDirectory) {
                        stack.add(child)
                    }
                } catch (_: Exception) {
                    // Ignore files we can't read
                }
            }
        }

        return totalSize
    }

    private fun queryDirectoryViaMediaStore(context: Context, directory: File): Pair<Int, Long>? {
        val storageRoot = Environment.getExternalStorageDirectory()
        val absolutePath = directory.absolutePath
        val relativePath = absolutePath
            .removePrefix(storageRoot.absolutePath)
            .trimStart(File.separatorChar)
            .replace(File.separatorChar, '/')

        if (relativePath.isEmpty()) {
            return null
        }

        val resolver = context.contentResolver
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)

        val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val targetPath = if (relativePath.endsWith('/')) relativePath else "$relativePath/"
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" to arrayOf("$targetPath%")
        } else {
            val directoryPrefix = absolutePath + File.separator
            "${MediaStore.MediaColumns.DATA} LIKE ?" to arrayOf("$directoryPrefix%")
        }

        var totalSize = 0L
        var itemCount = 0

        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                totalSize += cursor.getLong(sizeIndex)
                itemCount++
            }
        } ?: return null

        return Pair(itemCount, totalSize)
    }
}

