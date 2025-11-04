package com.filemanager.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class IconSpec(val icon: ImageVector, val tint: Color)

fun docIconForExt(ext: String): IconSpec = when (ext.lowercase()) {
    "pdf" -> IconSpec(Icons.Outlined.PictureAsPdf, Color(0xFFD32F2F))
    "doc","docx" -> IconSpec(Icons.Outlined.Description, Color(0xFF1565C0))
    "xls","xlsx","csv" -> IconSpec(Icons.Outlined.GridOn, Color(0xFF2E7D32))
    "ppt","pptx" -> IconSpec(Icons.Outlined.Slideshow, Color(0xFFF57C00))
    "txt","rtf","html","htm","epub" -> IconSpec(Icons.Outlined.Article, Color(0xFF6A1B9A))
    else -> IconSpec(Icons.Outlined.InsertDriveFile, Color(0xFF455A64))
}
