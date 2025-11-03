package com.filemanager.app.data

import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

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
            "tsv",
            "xps",
            "xml",
            "json",
            "html",
            "htm",
            "log",
            "cfg",
            "conf",
            "ini",
            "properties",
            "prop",
            "yaml",
            "yml",
            "md",
            "markdown",
            "tex",
            "epub",
            "mobi",
            "azw",
            "fb2",
            "chm",
            "wps",
            "wpt",
            "ps",
            "rtx",
            "odg",
            "numbers",
            "pages",
            "key",
            "sqlite",
            "db",
            "db3",
            "sql"
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
        val extension = file.extension.lowercase(Locale.getDefault())
        if (extensions.contains(extension)) {
            return true
        }

        if (this == DOCUMENTS) {
            if (extension in documentAdditionalExtensions) {
                return true
            }

            if (extension.isNotEmpty()) {
                val mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension)
                    ?.lowercase(Locale.getDefault())

                if (mimeType != null) {
                    if (mimeType.startsWith("text/")) {
                        return true
                    }

                    if (mimeType.startsWith("application/") &&
                        mimeType !in documentMimeExclusions
                    ) {
                        return true
                    }
                }
            }
        }

        return false
    }

    companion object {
        private val documentAdditionalExtensions = setOf(
            "bak",
            "backup",
            "lst",
            "nfo",
            "info",
            "cfg",
            "config",
            "bat",
            "sh",
            "py",
            "java",
            "kt",
            "c",
            "cpp",
            "h",
            "hpp",
            "gradle"
        )

        private val documentMimeExclusions = setOf(
            "application/zip",
            "application/x-7z-compressed",
            "application/x-rar-compressed",
            "application/x-tar",
            "application/gzip",
            "application/x-bzip2",
            "application/x-xz",
            "application/vnd.android.package-archive"
        )

        fun fromFile(file: File): FileCategory? {
            return values().find { it.matches(file) }
        }
    }
}

