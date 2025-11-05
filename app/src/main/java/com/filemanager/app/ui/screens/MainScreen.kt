package com.filemanager.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.QuickFilter
import com.filemanager.app.ui.viewmodel.FileManagerViewModel
import com.filemanager.app.utils.FileUtils
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FileManagerViewModel) {
    // Collect state from the ViewModel
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val storageState by viewModel.storageBrowserState.collectAsStateWithLifecycle()
    val quickFilterState by viewModel.quickFilterState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // --- FIX APPLIED IN THE BackHandler and Scaffold ---

    // System back handling
    BackHandler(enabled = quickFilterState != null) {
        viewModel.clearQuickFilter()
    }
    BackHandler(enabled = selectedCategory != null) {
        viewModel.clearCategorySelection()
    }
    BackHandler(enabled = storageState.currentPath != null) {
        viewModel.navigateStorageBack()
    }
    BackHandler(
        enabled =
            selectedCategory == null &&
            storageState.currentPath == null &&
            quickFilterState == null
    ) {
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { /* overflow menu */ }) {
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
                ) { CircularProgressIndicator() }
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

            // Now you can safely use the local copy
            quickFilterState != null -> {
                QuickFilterScreen(
                    state = quickFilterState, // No more smart cast error!
                    onBack = { viewModel.clearQuickFilter() },
                    modifier = Modifier.padding(padding)
                )
            }

            quickFilterState != null -> {
                QuickFilterScreen(
                    state = quickFilterState,
                    onBack = { viewModel.clearQuickFilter() },
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    QuickFilterRow(
                        selectedFilter = quickFilterState?.filter,
                        onFilterSelected = { filter -> viewModel.selectQuickFilter(filter) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HomeGridScreen(
                        categories = categories,
                        onCategoryClick = { viewModel.selectCategory(it) },
                        onStorageClick = { path -> viewModel.openStorage(path) },               // CHANGED
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit File Manager") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFilterRow(
    selectedFilter: QuickFilter?,
    onFilterSelected: (QuickFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickFilter.values().forEach { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = when (filter) {
                            QuickFilter.RECENT -> Icons.Default.History
                            QuickFilter.LARGE -> Icons.Default.UnfoldMore
                            QuickFilter.DUPLICATES -> Icons.Default.ContentCopy
                        },
                        contentDescription = null
                    )
                },
                shape = RoundedCornerShape(24.dp),
                // Keep only broadly-supported color args to avoid version mismatch
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Remove the custom BorderStroke; let defaults handle borders
                // border = FilterChipDefaults.filterChipBorder() // (optional if you want explicit)
            )
        }
    }
}


@Composable
private fun QuickFilterRow(
    selectedFilter: QuickFilter?,
    onFilterSelected: (QuickFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickFilter.values().forEach { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = when (filter) {
                            QuickFilter.RECENT -> Icons.Default.History
                            QuickFilter.LARGE -> Icons.Default.UnfoldMore
                            QuickFilter.DUPLICATES -> Icons.Default.ContentCopy
                        },
                        contentDescription = null
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            )
        }
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
@OptIn(ExperimentalMaterial3Api::class)
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
