package com.filemanager.app.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.filemanager.app.MainActivity
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.FileItem
import com.filemanager.app.ui.viewmodel.FileManagerViewModel
import com.filemanager.app.utils.FileUtils
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: FileCategory,
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var selectedFolders by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val categoryData = categories[category]
    val sources = categoryData?.sources ?: emptyMap()
    var showMoveCopyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)
    
    // Handle system back button
    BackHandler(enabled = true) {
        if (isSelectionMode || selectedFolders.isNotEmpty()) {
            viewModel.clearSelection()
            selectedFolders = emptySet()
        } else {
            viewModel.clearCategorySelection()
        }
    }
    
    // Filter sources based on search query
    val filteredSources = remember(sources, searchQuery) {
        if (searchQuery.isBlank()) {
            sources
        } else {
            sources.filter { (_, sourceData) ->
                sourceData.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.displayName) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode || selectedFolders.isNotEmpty()) {
                                viewModel.clearSelection()
                                selectedFolders = emptySet()
                            } else {
                                viewModel.clearCategorySelection()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF424242),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { /* Search functionality */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort/Filter")
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        bottomBar = {
            if (isSelectionMode || selectedFolders.isNotEmpty()) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedFolders = filteredSources.keys.toSet()
                        }) {
                            Text("Select All")
                        }
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                        TextButton(onClick = { showMoveCopyDialog = true }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Move")
                        }
                        TextButton(onClick = { showMoveCopyDialog = true }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }
                        TextButton(onClick = {
                            val files = selectedFolders.flatMap { path ->
                                filteredSources[path]?.files ?: emptyList()
                            }
                            (context as MainActivity).shareFiles(files.map { it.path })
                            viewModel.clearSelection()
                            selectedFolders = emptySet()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    ) { padding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.scanFiles() },
            modifier = modifier.fillMaxSize()
        ) {
            val sortedSources = remember(filteredSources) {
                filteredSources.entries.sortedBy { it.value.name.lowercase(Locale.getDefault()) }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedSources) { (path, sourceData) ->
                    FolderTile(
                        sourceData = sourceData,
                        category = category,
                        isSelected = selectedFolders.contains(path),
                        onLongPress = {
                            if (!isSelectionMode) {
                                viewModel.clearSelection()
                            }
                            selectedFolders = selectedFolders + path
                        },
                        onClick = {
                            if (isSelectionMode || selectedFolders.isNotEmpty()) {
                                selectedFolders = if (selectedFolders.contains(path)) {
                                    selectedFolders - path
                                } else {
                                    selectedFolders + path
                                }
                            } else {
                                // Navigate to folder contents
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Sort menu
    if (showSortMenu) {
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Name A-Z") },
                onClick = { showSortMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Name Z-A") },
                onClick = { showSortMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Size") },
                onClick = { showSortMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Date") },
                onClick = { showSortMenu = false }
            )
        }
    }
    
    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Folders") },
            text = { Text("Delete ${selectedFolders.size} folder(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val filesToDelete = selectedFolders.flatMap { path ->
                            filteredSources[path]?.files ?: emptyList()
                        }
                        val success = FileUtils.deleteFiles(filesToDelete)
                        showDeleteDialog = false
                        if (success) {
                            viewModel.clearSelection()
                            selectedFolders = emptySet()
                            viewModel.scanFiles()
                        }
                        Toast.makeText(
                            context,
                            if (success) "Folders deleted" else "Failed to delete some files",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Move/Copy dialog
    if (showMoveCopyDialog) {
        MoveCopyDialog(
            onDismiss = { showMoveCopyDialog = false },
            onMove = { destination ->
                val filesToMove = selectedFolders.flatMap { path ->
                    filteredSources[path]?.files ?: emptyList()
                }
                val success = FileUtils.moveFiles(filesToMove, destination)
                Toast.makeText(
                    context,
                    if (success) "Files moved" else "Failed to move some files",
                    Toast.LENGTH_SHORT
                ).show()
                if (success) {
                    viewModel.clearSelection()
                    selectedFolders = emptySet()
                    viewModel.scanFiles()
                    showMoveCopyDialog = false
                }
            },
            onCopy = { destination ->
                val filesToCopy = selectedFolders.flatMap { path ->
                    filteredSources[path]?.files ?: emptyList()
                }
                val success = FileUtils.copyFiles(filesToCopy, destination)
                Toast.makeText(
                    context,
                    if (success) "Files copied" else "Failed to copy some files",
                    Toast.LENGTH_SHORT
                ).show()
                if (success) {
                    viewModel.clearSelection()
                    selectedFolders = emptySet()
                    viewModel.scanFiles()
                    showMoveCopyDialog = false
                }
            }
        )
    }
}

@Composable
fun FolderTile(
    sourceData: com.filemanager.app.data.SourceFolderData,
    category: FileCategory,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val previewFile = remember(sourceData.files) {
        sourceData.files.firstOrNull()
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (previewFile != null && (category == FileCategory.IMAGES || category == FileCategory.VIDEOS)) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(File(previewFile.path))
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = previewFile.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForCategory(category),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${sourceData.name} (${sourceData.itemCount})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (sourceData.totalSize > 0) {
                    Text(
                        text = FileUtils.formatFileSize(sourceData.totalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getIconForCategory(category: FileCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.VideoLibrary
        FileCategory.AUDIO -> Icons.Default.MusicNote
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.APKS -> Icons.Default.Apps
        FileCategory.ARCHIVES -> Icons.Default.FolderZip
    }
}

@Composable
fun MoveCopyDialog(
    onDismiss: () -> Unit,
    onMove: (File) -> Unit,
    onCopy: (File) -> Unit
) {
    val destinations = remember {
        listOf(
            "Internal Storage" to Environment.getExternalStorageDirectory(),
            "Downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Movies" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        )
    }
    
    var selectedDestination by remember { mutableStateOf<File?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move/Copy Files") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Select destination:")
                Spacer(modifier = Modifier.height(8.dp))
                destinations.forEach { (name, file) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDestination = file }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDestination == file,
                            onClick = { selectedDestination = file }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        selectedDestination?.let { onCopy(it) }
                    },
                    enabled = selectedDestination != null
                ) {
                    Text("Copy")
                }
                TextButton(
                    onClick = {
                        selectedDestination?.let { onMove(it) }
                    },
                    enabled = selectedDestination != null
                ) {
                    Text("Move")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
