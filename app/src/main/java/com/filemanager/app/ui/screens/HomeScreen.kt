package com.filemanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.HomeItem
import com.filemanager.app.utils.FileUtils

@Composable
fun HomeGridScreen(
    categories: Map<FileCategory, com.filemanager.app.data.CategoryData>,
    onCategoryClick: (FileCategory) -> Unit,
    onStorageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storagePath = android.os.Environment.getExternalStorageDirectory().absolutePath
    val downloadsPath = android.os.Environment
        .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        ?.absolutePath
    val storageInfo = remember { FileUtils.getStorageInfo(storagePath) }
    val downloadsInfo = remember { FileUtils.getDownloadsInfo() }
    
    // Build home items list
    val homeItems = remember(categories, storageInfo, downloadsInfo) {
        buildList {
            // Storage items
            add(
                HomeItem.StorageItem(
                    title = "Main storage",
                    subtitle = "${FileUtils.formatFileSize(storageInfo.second)} / ${FileUtils.formatFileSize(storageInfo.first)}",
                    icon = "storage",
                    path = storagePath,
                    totalSpace = storageInfo.first,
                    usedSpace = storageInfo.second
                )
            )
            
            // Downloads
            downloadsPath?.let { path ->
                add(
                    HomeItem.DownloadsItem(
                        title = "Downloads",
                        subtitle = "${FileUtils.formatFileSize(downloadsInfo.second)} (${downloadsInfo.first})",
                        icon = "downloads",
                        itemCount = downloadsInfo.first,
                        totalSize = downloadsInfo.second,
                        path = path
                    )
                )
            }
            
            // Categories
            add(
                HomeItem.CategoryItem(
                    title = "Images",
                    subtitle = categories[FileCategory.IMAGES]?.let { 
                        "${FileUtils.formatFileSize(it.totalSize)} (${it.itemCount})" 
                    } ?: "0 B (0)",
                    icon = "images",
                    category = FileCategory.IMAGES,
                    itemCount = categories[FileCategory.IMAGES]?.itemCount ?: 0,
                    totalSize = categories[FileCategory.IMAGES]?.totalSize ?: 0
                )
            )
            
            add(
                HomeItem.CategoryItem(
                    title = "Audio",
                    subtitle = categories[FileCategory.AUDIO]?.let { 
                        "${FileUtils.formatFileSize(it.totalSize)} (${it.itemCount})" 
                    } ?: "0 B (0)",
                    icon = "audio",
                    category = FileCategory.AUDIO,
                    itemCount = categories[FileCategory.AUDIO]?.itemCount ?: 0,
                    totalSize = categories[FileCategory.AUDIO]?.totalSize ?: 0
                )
            )
            
            add(
                HomeItem.CategoryItem(
                    title = "Videos",
                    subtitle = categories[FileCategory.VIDEOS]?.let { 
                        "${FileUtils.formatFileSize(it.totalSize)} (${it.itemCount})" 
                    } ?: "0 B (0)",
                    icon = "videos",
                    category = FileCategory.VIDEOS,
                    itemCount = categories[FileCategory.VIDEOS]?.itemCount ?: 0,
                    totalSize = categories[FileCategory.VIDEOS]?.totalSize ?: 0
                )
            )
            
            add(
                HomeItem.CategoryItem(
                    title = "Documents",
                    subtitle = categories[FileCategory.DOCUMENTS]?.let { 
                        "${FileUtils.formatFileSize(it.totalSize)} (${it.itemCount})" 
                    } ?: "0 B (0)",
                    icon = "documents",
                    category = FileCategory.DOCUMENTS,
                    itemCount = categories[FileCategory.DOCUMENTS]?.itemCount ?: 0,
                    totalSize = categories[FileCategory.DOCUMENTS]?.totalSize ?: 0
                )
            )
            
            add(
                HomeItem.CategoryItem(
                    title = "Apps",
                    subtitle = categories[FileCategory.APKS]?.let { 
                        "${FileUtils.formatFileSize(it.totalSize)} (${it.itemCount})" 
                    } ?: "0 B (0)",
                    icon = "apps",
                    category = FileCategory.APKS,
                    itemCount = categories[FileCategory.APKS]?.itemCount ?: 0,
                    totalSize = categories[FileCategory.APKS]?.totalSize ?: 0
                )
            )
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(homeItems) { item ->
            HomeGridItem(
                item = item,
                onClick = {
                    when (item) {
                        is HomeItem.CategoryItem -> onCategoryClick(item.category)
                        is HomeItem.StorageItem -> onStorageClick(item.path)
                        is HomeItem.DownloadsItem -> onStorageClick(item.path)
                    }
                }
            )
        }
    }
}

@Composable
fun HomeGridItem(
    item: HomeItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val iconSize = 36.dp
            val titleStyle = MaterialTheme.typography.titleMedium
            val subtitleStyle = MaterialTheme.typography.bodySmall

            Icon(
                imageVector = getIconForItem(item),
                contentDescription = item.title,
                modifier = Modifier.size(iconSize),
                tint = getIconColorForItem(item)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = titleStyle.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = item.subtitle.ifBlank { "0 B (0)" },
                    style = subtitleStyle.copy(
                        lineHeight = subtitleStyle.fontSize * 1.3f
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
        }
    }
}

@Composable
fun getIconForItem(item: HomeItem): ImageVector {
    return when (item) {
        is HomeItem.StorageItem -> Icons.Default.Storage
        is HomeItem.DownloadsItem -> Icons.Default.Download
        is HomeItem.CategoryItem -> getIconForCategory(item.category)
    }
}

@Composable
fun getIconColorForItem(item: HomeItem): Color {
    return when (item) {
        is HomeItem.StorageItem -> Color(0xFF9E9E9E)
        is HomeItem.DownloadsItem -> Color(0xFF795548)
        is HomeItem.CategoryItem -> categoryAccentColor(item.category)
    }
}

@Composable
fun categoryAccentColor(category: FileCategory): Color {
    return when (category) {
        FileCategory.IMAGES -> Color(0xFF9C27B0)
        FileCategory.VIDEOS -> Color(0xFFF44336)
        FileCategory.AUDIO -> Color(0xFF009688)
        FileCategory.DOCUMENTS -> Color(0xFF2196F3)
        FileCategory.APKS -> Color(0xFF4CAF50)
        FileCategory.ARCHIVES -> Color(0xFF607D8B)
    }
}

