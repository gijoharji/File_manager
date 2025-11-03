package com.filemanager.app.data

import java.io.File

enum class FileCategory(val displayName: String, val extensions: Set<String>) {
    IMAGES(
        "Images",
        setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg", "ico")
    ),
    VIDEOS(
        "Videos",
        setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts")
    ),
    AUDIO(
        "Audio",
        setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "amr")
    ),
    DOCUMENTS(
        "Documents",
        setOf(
            "pdf",
            "doc",
            "docx",
            "docm",
            "dot",
            "dotx",
            "dotm",
            "xls",
            "xlsx",
            "xlsm",
            "xlsb",
            "ppt",
            "pptx",
            "pptm",
            "pps",
            "ppsx",
            "ppsm",
            "pot",
            "potx",
            "potm",
            "txt",
            "rtf",
            "odt",
            "ods",
            "odp",
            "csv",
            "xps"
        )
    ),
    APKS(
        "APKs",
        setOf("apk")
    ),
    ARCHIVES(
        "Archives",
        setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    );

    fun matches(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extensions.contains(extension)
    }

    companion object {
        fun fromFile(file: File): FileCategory? {
            return values().find { it.matches(file) }
        }
    }
}

