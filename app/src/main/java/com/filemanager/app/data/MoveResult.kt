package com.filemanager.app.data

data class MoveResult(
    val moved: Int,
    val skipped: Int,
    val failed: List<Pair<String, String>>
)
