package com.filemanager.app.data

sealed class HomeItem {
    abstract val title: String
    abstract val subtitle: String
    abstract val icon: String
    
    data class StorageItem(
        override val title: String,
        override val subtitle: String,
        override val icon: String,
        val path: String,
        val totalSpace: Long,
        val usedSpace: Long
    ) : HomeItem()
    
    data class CategoryItem(
        override val title: String,
        override val subtitle: String,
        override val icon: String,
        val category: FileCategory,
        val itemCount: Int,
        val totalSize: Long
    ) : HomeItem()
    
    data class DownloadsItem(
        override val title: String,
        override val subtitle: String,
        override val icon: String,
        val itemCount: Int,
        val totalSize: Long,
        val path: String
    ) : HomeItem()
}

