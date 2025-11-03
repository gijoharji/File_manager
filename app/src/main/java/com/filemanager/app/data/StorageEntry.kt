package com.filemanager.app.data

data class StorageEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val itemCount: Int,
    val lastModified: Long
)
