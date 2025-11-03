package com.filemanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filemanager.app.data.FileCategory
import com.filemanager.app.ui.viewmodel.FileManagerViewModel
import com.filemanager.app.utils.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FileManagerViewModel) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val storageState by viewModel.storageBrowserState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Handle system back button
    BackHandler(enabled = selectedCategory != null) {
        viewModel.clearCategorySelection()
    }

    BackHandler(enabled = storageState.currentPath != null) {
        viewModel.navigateStorageBack()
    }

    BackHandler(enabled = selectedCategory == null && storageState.currentPath == null) {
        showExitDialog = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Manager +") },
                navigationIcon = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF424242),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { 
                        // Overflow menu options
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            storageState.currentPath != null -> {
                StorageBrowserScreen(
                    currentPath = storageState.currentPath!!,
                    navigationStack = storageState.stack,
                    entries = storageState.entries,
                    isLoading = storageState.isLoading,
                    onNavigateUp = { viewModel.navigateStorageBack() },
                    onFolderClick = { entry -> viewModel.navigateIntoStorage(entry.path) },
                    onClose = { viewModel.closeStorageBrowser() },
                    modifier = Modifier.padding(padding)
                )
            }
            selectedCategory != null -> {
                CategoryDetailScreen(
                    category = selectedCategory!!,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                HomeGridScreen(
                    categories = categories,
                    onCategoryClick = { viewModel.selectCategory(it) },
                    onStorageClick = { path -> viewModel.showStorageRoot(path) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit File Manager") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        (context as? android.app.Activity)?.finish()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryListScreen(
    categories: Map<FileCategory, com.filemanager.app.data.CategoryData>,
    onCategoryClick: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(FileCategory.values().toList()) { category ->
            val categoryData = categories[category]
            CategoryCard(
                category = category,
                itemCount = categoryData?.itemCount ?: 0,
                totalSize = categoryData?.totalSize ?: 0,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: FileCategory,
    itemCount: Int,
    totalSize: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$itemCount items • ${FileUtils.formatFileSize(totalSize)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.3f,
                        letterSpacing = (-0.1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

