package com.filemanager.app.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.filemanager.app.MainActivity
import com.filemanager.app.data.DestPreset
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.FileItem
import com.filemanager.app.data.SourceFolderData
import com.filemanager.app.data.StorageEntry
import com.filemanager.app.ui.viewmodel.FileManagerViewModel
import com.filemanager.app.utils.FileUtils
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.io.File
import java.util.Locale
import androidx.compose.material3.RadioButton
import com.filemanager.app.ui.icons.docIconForExt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BottomBarDisabledAlpha = 0.38f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: FileCategory,
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var selectedFolders by remember { mutableStateOf(setOf<String>()) }
    var currentFolderPath by remember { mutableStateOf<String?>(null) }
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val thumbnailImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
    DisposableEffect(thumbnailImageLoader) {
        onDispose { thumbnailImageLoader.shutdown() }
    }

    val categoryData = categories[category]
    val sources = categoryData?.sources ?: emptyMap()
    var showMoveCopyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFileMoreMenu by remember { mutableStateOf(false) }
    var showFolderMoreMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)
    
    // Handle system back button
    BackHandler(enabled = true) {
        when {
            isSelectionMode -> viewModel.clearSelection()
            selectedFolders.isNotEmpty() -> selectedFolders = emptySet()
            currentFolderPath != null -> {
                currentFolderPath = null
                viewModel.clearSelection()
            }
            else -> viewModel.clearCategorySelection()
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

    val currentFolderData = remember(filteredSources, currentFolderPath) {
        currentFolderPath?.let { filteredSources[it] }
    }

    LaunchedEffect(filteredSources, currentFolderPath) {
        if (currentFolderPath != null && currentFolderData == null) {
            currentFolderPath = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when {
                        currentFolderData != null -> "${currentFolderData.name} (${currentFolderData.itemCount})"
                        else -> category.displayName
                    }
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                isSelectionMode -> viewModel.clearSelection()
                                selectedFolders.isNotEmpty() -> selectedFolders = emptySet()
                                currentFolderPath != null -> {
                                    currentFolderPath = null
                                    viewModel.clearSelection()
                                }
                                else -> viewModel.clearCategorySelection()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { /* Search functionality */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    if (currentFolderData == null) {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort/Filter")
                        }
                        IconButton(onClick = { /* More options */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentFolderData != null && isSelectionMode) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Copy",
                            icon = Icons.Default.ContentCopy,
                            enabled = selectedFiles.isNotEmpty(),
                            onClick = { showMoveCopyDialog = true }
                        )
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Move",
                            icon = Icons.Default.DriveFileMove,
                            enabled = selectedFiles.isNotEmpty(),
                            onClick = { showMoveCopyDialog = true }
                        )
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Rename",
                            icon = Icons.Default.Edit,
                            enabled = selectedFiles.size == 1,
                            onClick = { showRenameDialog = true }
                        )
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Delete",
                            icon = Icons.Default.Delete,
                            enabled = selectedFiles.isNotEmpty(),
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true }
                        )
                        BottomBarMoreAction(
                            modifier = Modifier.weight(1f),
                            enabled = selectedFiles.isNotEmpty(),
                            expanded = showFileMoreMenu,
                            onExpandedChange = { showFileMoreMenu = it },
                            onShare = {
                                showFileMoreMenu = false
                                if (selectedFiles.isNotEmpty()) {
                                    (context as? MainActivity)?.shareFiles(selectedFiles.toList())
                                    viewModel.clearSelection()
                                }
                            },
                            onProperties = {
                                showFileMoreMenu = false
                                Toast.makeText(
                                    context,
                                    "Properties coming soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            } else if (selectedFolders.isNotEmpty()) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "All",
                            icon = Icons.Default.SelectAll,
                            onClick = {
                                selectedFolders = filteredSources.keys.toSet()
                            }
                        )
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Delete",
                            icon = Icons.Default.Delete,
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true }
                        )
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Move",
                            icon = Icons.Default.DriveFileMove,
                            onClick = { showMoveCopyDialog = true }
                        )
                        BottomBarActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Copy",
                            icon = Icons.Default.ContentCopy,
                            onClick = { showMoveCopyDialog = true }
                        )
                        BottomBarMoreAction(
                            modifier = Modifier.weight(1f),
                            expanded = showFolderMoreMenu,
                            onExpandedChange = { showFolderMoreMenu = it },
                            onShare = {
                                showFolderMoreMenu = false
                                val files = selectedFolders.flatMap { path ->
                                    filteredSources[path]?.files ?: emptyList()
                                }
                                if (files.isNotEmpty()) {
                                    (context as? MainActivity)?.shareFiles(files.map { it.path })
                                    viewModel.clearSelection()
                                    selectedFolders = emptySet()
                                }
                            },
                            onProperties = {
                                showFolderMoreMenu = false
                                Toast.makeText(
                                    context,
                                    "Properties coming soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.scanFiles() },
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentFolderData != null) {
                val files = currentFolderData.files
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(files) { fileItem ->
                        FileTile(
                            fileItem = fileItem,
                            category = fileItem.category ?: category,
                            isSelected = selectedFiles.contains(fileItem.path),
                            imageLoader = thumbnailImageLoader,
                            onLongPress = {
                                viewModel.toggleFileSelection(fileItem.path)
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleFileSelection(fileItem.path)
                                } else {
                                    (context as? MainActivity)?.openFileWith(fileItem.path)
                                }
                            }
                        )
                    }
                }
            } else {
                val sortedSources = remember(filteredSources) {
                    filteredSources.entries.sortedBy { it.value.name.lowercase(Locale.getDefault()) }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedSources) { (path, sourceData) ->
                        FolderTile(
                            sourceData = sourceData,
                            category = category,
                            isSelected = selectedFolders.contains(path),
                            imageLoader = thumbnailImageLoader,
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
                                    currentFolderPath = path
                                    selectedFolders = emptySet()
                                    viewModel.clearSelection()
                                }
                            }
                        )
                    }
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
        val deleteCount = if (currentFolderData != null) selectedFiles.size else selectedFolders.size
        val deleteTitle = if (currentFolderData != null) "Delete Files" else "Delete Folders"
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteTitle) },
            text = {
                if (currentFolderData != null) {
                    Text("Delete $deleteCount file(s)? This action cannot be undone.")
                } else {
                    Text("Delete $deleteCount folder(s)? This action cannot be undone.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = if (currentFolderData != null) {
                            viewModel.deleteSelectedFiles()
                        } else {
                            val filesToDelete = selectedFolders.flatMap { path ->
                                filteredSources[path]?.files ?: emptyList()
                            }
                            FileUtils.deleteFiles(filesToDelete).also { result ->
                                if (result) {
                                    viewModel.clearSelection()
                                    selectedFolders = emptySet()
                                    viewModel.scanFiles()
                                }
                            }
                        }
                        showDeleteDialog = false
                        Toast.makeText(
                            context,
                            if (success) {
                                if (currentFolderData != null) "Files deleted" else "Folders deleted"
                            } else {
                                "Failed to delete some files"
                            },
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
        val filesForAction = if (currentFolderData != null) {
            viewModel.getSelectedFileItems()
        } else {
            selectedFolders.flatMap { path ->
                filteredSources[path]?.files ?: emptyList()
            }
        }
        MoveCopyDialog(
            onDismiss = { showMoveCopyDialog = false },
            onMove = { preset ->
                val appContext = context.applicationContext
                scope.launch {
                    val entriesForMove = withContext(Dispatchers.IO) {
                        filesForAction.map { it.toStorageEntry() }
                    }
                    val result = FileUtils.moveEntriesToPreset(
                        context = appContext,
                        entries = entriesForMove,
                        preset = preset,
                        replaceIfExists = false
                    )

                    if (result.failed.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Moved ${'$'}{result.moved} item(s)",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.clearSelection()
                        selectedFolders = emptySet()
                        viewModel.scanFiles()
                        showMoveCopyDialog = false
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to move ${'$'}{result.failed.size} item(s)",
                            Toast.LENGTH_SHORT
                        ).show()
                        result.failed.forEach { (path, reason) ->
                            Log.w("Move", "Fail: ${'$'}path -> ${'$'}reason")
                        }
                        if (result.moved > 0) {
                            viewModel.scanFiles()
                        }
                    }
                }
            },
            onCopy = { preset ->
                val appContext = context.applicationContext
                scope.launch {
                    val destination = FileUtils.getPresetDirectory(appContext, preset).apply { mkdirs() }
                    val success = withContext(Dispatchers.IO) {
                        FileUtils.copyFiles(filesForAction, destination)
                    }
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
            }
        )
    }

    if (showRenameDialog) {
        val selectedFileName = viewModel.getSelectedFileItems().firstOrNull()?.name ?: ""
        RenameFileDialog(
            initialName = selectedFileName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                val success = viewModel.renameSelectedFile(newName)
                Toast.makeText(
                    context,
                    if (success) "File renamed" else "Failed to rename file",
                    Toast.LENGTH_SHORT
                ).show()
                showRenameDialog = false
            }
        )
    }
}

@Composable
private fun BottomBarActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val displayColor = if (enabled) {
        contentColor
    } else {
        contentColor.copy(alpha = BottomBarDisabledAlpha)
    }

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = displayColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = displayColor
            )
        }
    }
}

@Composable
private fun BottomBarMoreAction(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onProperties: () -> Unit
) {
    val displayColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = BottomBarDisabledAlpha)
    }

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled) { onExpandedChange(true) }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = displayColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "More",
                style = MaterialTheme.typography.labelMedium,
                color = displayColor
            )
        }

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text("Properties") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onProperties()
                }
            )
        }
    }
}

@Composable
fun FileTile(
    fileItem: FileItem,
    category: FileCategory,
    isSelected: Boolean,
    imageLoader: ImageLoader,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.7f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isSelected) 0.0f else 0.5f)
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
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (category == FileCategory.IMAGES || category == FileCategory.VIDEOS) {
                    val context = LocalContext.current
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(File(fileItem.path))
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader
                    )
                    Image(
                        painter = painter,
                        contentDescription = fileItem.name,
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
                        if (category == FileCategory.DOCUMENTS) {
                            // Show a per-extension document icon (PDF/Word/Excel/PPT/TXT…)
                            val ext = fileItem.name.substringAfterLast('.', "").lowercase(Locale.getDefault())
                            val spec = docIconForExt(ext) // <- your mapper
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = spec.tint
                            )
                        } else {
                            // Non-document generic category icon (Audio/Archives/APKs)
                            Icon(
                                imageVector = getIconForCategory(category),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }


                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = FileUtils.formatFileSize(fileItem.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RenameFileDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            Column {
                Text("Enter a new name for the file:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(fileName) },
                enabled = fileName.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FolderTile(
    sourceData: SourceFolderData,
    category: FileCategory,
    isSelected: Boolean,
    imageLoader: ImageLoader, // kept to avoid call-site changes
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isSelected) 0.0f else 0.5f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER AREA: ONLY a blue folder icon (no preview, no grey doc icon) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),            // blue folder
                    modifier = Modifier.size(40.dp)
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    )
                }
            }

            // --- TEXT AREA: Name, then (quantity), then size ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = sourceData.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "(${sourceData.itemCount})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (sourceData.totalSize > 0) {
                    Text(
                        text = FileUtils.formatFileSize(sourceData.totalSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    onMove: (DestPreset) -> Unit,
    onCopy: (DestPreset) -> Unit
) {
    val destinations = remember { DestPreset.values().toList() }

    var selectedDestination by remember { mutableStateOf<DestPreset?>(null) }
    
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
                destinations.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDestination = preset }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDestination == preset,
                            onClick = { selectedDestination = preset }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(preset.displayName)
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

private fun FileItem.toStorageEntry(): StorageEntry {
    val file = File(path)
    val isDir = file.isDirectory
    val actualName = file.name.takeIf { it.isNotBlank() } ?: name
    val extension = if (!isDir) actualName.substringAfterLast('.', "").lowercase(Locale.getDefault()) else ""
    val sizeValue = if (!isDir && file.exists()) file.length() else size
    val lastModifiedValue = if (file.exists()) file.lastModified() else dateModified
    val count = if (isDir) file.listFiles()?.size ?: 0 else 0

    return StorageEntry(
        path = path,
        name = actualName,
        isDirectory = isDir,
        size = sizeValue,
        itemCount = count,
        lastModified = lastModifiedValue,
        extension = extension
    )
}
