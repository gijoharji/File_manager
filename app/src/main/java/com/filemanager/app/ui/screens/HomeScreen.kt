package com.filemanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.HomeItem
import com.filemanager.app.utils.FileUtils
import java.io.File

@Composable
fun HomeGridScreen(
    categories: Map<FileCategory, com.filemanager.app.data.CategoryData>,
    onCategoryClick: (FileCategory) -> Unit,
    onStorageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storagePath = android.os.Environment.getExternalStorageDirectory().absolutePath
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
            add(
                HomeItem.DownloadsItem(
                    title = "Downloads",
                    subtitle = "${FileUtils.formatFileSize(downloadsInfo.second)} (${downloadsInfo.first})",
                    icon = "downloads",
                    itemCount = downloadsInfo.first,
                    totalSize = downloadsInfo.second
                )
            )
            
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
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(homeItems) { item ->
            HomeGridItem(
                item = item,
                onClick = {
                    when (item) {
                        is HomeItem.CategoryItem -> onCategoryClick(item.category)
                        is HomeItem.StorageItem -> onStorageClick(item.path)
                        is HomeItem.DownloadsItem -> {
                            // Handle downloads click
                            onCategoryClick(FileCategory.IMAGES) // Navigate to Downloads folder
                        }
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
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp)
        ) {
            val density = LocalDensity.current
            val iconSize = 36.dp  // Reduced icon size
            // Use Material typography for proper text scaling support
            val titleStyle = MaterialTheme.typography.titleMedium
            val subtitleStyle = MaterialTheme.typography.bodySmall
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with icon
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Icon - reduced size
                    Icon(
                        imageVector = getIconForItem(item),
                        contentDescription = item.title,
                        modifier = Modifier.size(iconSize),
                        tint = getIconColorForItem(item)
                    )
                }
                
                // Bottom section with text - reserved space
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Title
                    Text(
                        text = item.title,
                        style = titleStyle.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Subtitle - ensure it's fully visible with text scaling support
                    Text(
                        text = item.subtitle.ifBlank { "0 B (0)" },
                        style = subtitleStyle.copy(
                            lineHeight = subtitleStyle.fontSize * 1.4f,
                            letterSpacing = (-0.1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.dp)
                            .wrapContentHeight(),
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true
                    )
                }
            }
        }
    }
}

@Composable
fun getIconForItem(item: HomeItem): ImageVector {
    return when (item.icon) {
        "storage" -> Icons.Default.Storage
        "downloads" -> Icons.Default.Download
        "images" -> Icons.Default.Image
        "audio" -> Icons.Default.MusicNote
        "videos" -> Icons.Default.VideoLibrary
        "documents" -> Icons.Default.Description
        "apps" -> Icons.Default.Apps
        else -> Icons.Default.Folder
    }
}

@Composable
fun getIconColorForItem(item: HomeItem): Color {
    return when (item.icon) {
        "storage" -> Color(0xFF9E9E9E)
        "downloads" -> Color(0xFF795548)
        "images" -> Color(0xFF9C27B0)
        "audio" -> Color(0xFF009688)
        "videos" -> Color(0xFFF44336)
        "documents" -> Color(0xFF2196F3)
        "apps" -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }
}

