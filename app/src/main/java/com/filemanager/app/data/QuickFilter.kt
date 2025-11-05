package com.filemanager.app.data

enum class QuickFilter(val displayName: String) {
    RECENT("Recent"),
    LARGE("Large"),
    DUPLICATES("Duplicates");
}

data class QuickFilterGroup(
    val title: String?,
    val files: List<FileItem>
)

data class QuickFilterState(
    val filter: QuickFilter,
    val groups: List<QuickFilterGroup>
)
