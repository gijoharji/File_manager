package com.filemanager.app.data

data class CategoryData(
    val category: FileCategory,
    val itemCount: Int,
    val totalSize: Long,
    val sources: Map<String, SourceFolderData>
)

data class SourceFolderData(
    val path: String,
    val name: String,
    val itemCount: Int,
    val totalSize: Long,
    val files: List<FileItem>
)

data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val category: FileCategory?
)

