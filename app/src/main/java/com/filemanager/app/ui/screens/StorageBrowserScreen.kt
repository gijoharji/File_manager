package com.filemanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filemanager.app.data.StorageEntry
import com.filemanager.app.utils.FileUtils
import java.io.File

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

    Column(modifier = modifier.fillMaxSize()) {
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
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(entries, key = { it.path }) { entry ->
                            StorageEntryRow(
                                entry = entry,
                                onClick = {
                                    if (entry.isDirectory) {
                                        onFolderClick(entry)
                                    }
                                }
                            )
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageEntryRow(
    entry: StorageEntry,
    onClick: () -> Unit
) {
    val icon = if (entry.isDirectory) {
        Icons.Default.Folder
    } else {
        Icons.Default.Description
    }

    val subtitle = if (entry.isDirectory) {
        val sizeText = FileUtils.formatFileSize(entry.size)
        val countText = if (entry.itemCount == 1) "1 item" else "${entry.itemCount} items"
        "$countText • $sizeText"
    } else {
        FileUtils.formatFileSize(entry.size)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.isDirectory, onClick = onClick),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            RowHeader(entry = entry, iconName = icon)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowHeader(entry: StorageEntry, iconName: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = iconName,
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
