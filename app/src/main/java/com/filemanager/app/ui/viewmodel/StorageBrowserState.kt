package com.filemanager.app.ui.viewmodel

import com.filemanager.app.data.StorageEntry

data class StorageBrowserState(
    val stack: List<String> = emptyList(),
    val currentPath: String? = null,
    val entries: List<StorageEntry> = emptyList(),
    val isLoading: Boolean = false
)
