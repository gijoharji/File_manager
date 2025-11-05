package com.filemanager.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.filemanager.app.MainActivity
import com.filemanager.app.data.StorageEntry
import com.filemanager.app.utils.FileUtils
import java.io.File
import java.util.Locale

@Composable
private fun StorageEntryGridItem(
    entry: StorageEntry,    onClick: (StorageEntry) -> Unit, // <<< ONLY ONE CLICK HANDLER
    modifier: Modifier = Modifier
)
 {
    // --- icon by type ---
    // ... inside StorageEntryGridItem
    val (icon, tint) = if (entry.isDirectory) {
        // If it's a directory, use the folder icon and primary color
        Icons.Default.Folder to MaterialTheme.colorScheme.primary
    } else {
        // If it's a file, get the spec from our helper function
        val ext = entry.name.substringAfterLast('.', "").lowercase(Locale.getDefault())
        val spec = iconForExt(ext)

        // CORRECTED: Assign the icon and tint from the returned IconSpec
        spec.icon to spec.tint
    }


    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(entry) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        val subtitle = if (entry.isDirectory) {
            val count = if (entry.itemCount == 1) "1 item" else "${entry.itemCount} items"
            count
        } else {
            FileUtils.formatFileSize(entry.size)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = entry.name,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class IconSpec(val icon: ImageVector, val tint: Color)

// Map file extension -> icon + color
private fun iconForExt(extRaw: String): IconSpec = when (extRaw.lowercase()) {
    "pdf"                 -> IconSpec(Icons.Outlined.PictureAsPdf, Color(0xFFD32F2F)) // red
    "doc", "docx"         -> IconSpec(Icons.Outlined.Description,  Color(0xFF1565C0)) // blue
    "xls", "xlsx", "csv"  -> IconSpec(Icons.Outlined.GridOn,       Color(0xFF2E7D32)) // green
    "ppt", "pptx"         -> IconSpec(Icons.Outlined.Slideshow,    Color(0xFFF57C00)) // orange
    "txt"                 -> IconSpec(Icons.Outlined.Article,      Color(0xFF616161)) // gray
    "rtf","html","htm","epub" ->
        IconSpec(Icons.Outlined.Article,      Color(0xFF6A1B9A)) // purple
    else                  -> IconSpec(Icons.Outlined.InsertDriveFile, Color(0xFF455A64))
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageBrowserScreen(
    currentPath: String,
    navigationStack: List<String>,
    entries: List<StorageEntry>,
    isLoading: Boolean,
    onNavigateUp: () -> Unit,
    onFolderClick: (StorageEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAtRoot = navigationStack.size <= 1
    val title = if (isAtRoot) {
        "Main storage"
    } else {
        File(currentPath).name.takeIf { it.isNotBlank() } ?: currentPath
    }
    val breadcrumb = navigationStack.mapIndexed { index, path ->
        val file = File(path)
        val name = file.name
        when {
            index == 0 -> "Main storage"
            name.isNotBlank() -> name
            else -> path
        }
    }.joinToString(separator = " / ")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title)
                    Text(
                        text = breadcrumb,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { if (isAtRoot) onClose() else onNavigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = if (isAtRoot) "Close" else "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                entries.isEmpty() && !isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    val context = LocalContext.current

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(entries, key = { it.path }) { entry ->
                            StorageEntryGridItem(
                                entry = entry,
                                onClick = { clickedEntry ->
                                    if (clickedEntry.isDirectory) {
                                        onFolderClick(clickedEntry)
                                    } else {
                                        (context as? MainActivity)?.openFileWith(clickedEntry.path)
                                    }
                                }
                            )
                        }


                    }


                }
            }
        }


    }

}
