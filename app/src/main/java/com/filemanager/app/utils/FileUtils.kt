package com.filemanager.app.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import com.filemanager.app.data.CategoryData
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.FileItem
import com.filemanager.app.data.SourceFolderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    
    private val commonSources = mapOf(
        "Downloads" to listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        ),
        "WhatsApp" to listOf(
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp")
        ),
        "Screenshots" to listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            File(Environment.getExternalStorageDirectory(), "Pictures/Screenshots"),
            File(Environment.getExternalStorageDirectory(), "DCIM/Screenshots")
        ),
        "Camera" to listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        )
    )

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

        // Build result
        categoryMap.mapValues { (category, sourceMap) ->
            val totalFiles = sourceMap.values.flatten()
            val totalSize = totalFiles.sumOf { it.size }
            
            val sources = sourceMap.mapValues { (path, files) ->
                val folderName = getFolderDisplayName(path)
                SourceFolderData(
                    path = path,
                    name = folderName,
                    itemCount = files.size,
                    totalSize = files.sumOf { it.size },
                    files = files.sortedByDescending { it.dateModified }
                )
            }

            CategoryData(
                category = category,
                itemCount = totalFiles.size,
                totalSize = totalSize,
                sources = sources.toSortedMap(compareBy { it })
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
                    val category = FileCategory.fromFile(file)
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
                    // Skip some system directories
                    if (file.name.startsWith(".") || 
                        file.name == "Android" ||
                        file.name == "Lost.Dir" ||
                        file.name == "LOST.DIR") {
                        continue
                    }
                    scanDirectoryRecursive(file, categoryMap)
                }
            }
        } catch (e: Exception) {
            // Skip directories we can't access
        }
    }

    private fun getSourcePath(file: File): String {
        val absolutePath = file.absolutePath
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath
        
        // Check common sources
        for ((name, paths) in commonSources) {
            for (sourcePath in paths) {
                if (absolutePath.startsWith(sourcePath.absolutePath)) {
                    return sourcePath.absolutePath
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
        
        // Check if it's a known common source
        for ((displayName, paths) in commonSources) {
            if (paths.any { it.absolutePath == path }) {
                return displayName
            }
        }
        
        // Use folder name or parent folder name
        if (name.isNotEmpty() && name != "storage" && name != "emulated" && name != "0") {
            return name
        }
        
        val parent = file.parentFile
        return parent?.name ?: "Other"
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
}

