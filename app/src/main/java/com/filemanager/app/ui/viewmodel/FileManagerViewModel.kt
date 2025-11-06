package com.filemanager.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemanager.app.data.CategoryData
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.FileItem
import com.filemanager.app.data.QuickFilter
import com.filemanager.app.data.QuickFilterGroup
import com.filemanager.app.data.QuickFilterState
import com.filemanager.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    // ---------- Categories / selection ----------
    private val _categories = MutableStateFlow<Map<FileCategory, CategoryData>>(emptyMap())
    val categories: StateFlow<Map<FileCategory, CategoryData>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _quickFilterState = MutableStateFlow<QuickFilterState?>(null)
    val quickFilterState: StateFlow<QuickFilterState?> = _quickFilterState.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _storageBrowserState = MutableStateFlow(StorageBrowserState())
    val storageBrowserState: StateFlow<StorageBrowserState> = _storageBrowserState.asStateFlow()

    init {
        scanFiles()
    }

    // ===== Categories and scanning =====
    fun scanFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val scanned = FileUtils.scanFiles(getApplication())
                _categories.value = scanned
                refreshQuickFilter()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCategory(category: FileCategory) {
        _selectedCategory.value = category
        clearSelection()
    }

    fun clearCategorySelection() {
        _selectedCategory.value = null
        clearSelection()
    }

    fun selectQuickFilter(filter: QuickFilter) {
        clearSelection()
        _selectedCategory.value = null
        _quickFilterState.value = buildQuickFilterState(filter)
    }

    fun clearQuickFilter() {
        _quickFilterState.value = null
    }

    fun openStorageRoot(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun openStorageFolder(path: String) {
        val currentStack = _storageBrowserState.value.stack
        if (currentStack.isNotEmpty() && currentStack.last() == path) {
            return
        }

        setStorageContext(currentStack + path)
    }

    fun navigateStorageBack(): Boolean {
        val stack = _storageBrowserState.value.stack
        return when {
            stack.size > 1 -> {
                setStorageContext(stack.dropLast(1))
                true
            }

            stack.isNotEmpty() -> {
                closeStorageBrowser()
                true
            }

            else -> false
        }
    }

    fun closeStorageBrowser() {
        clearSelection()
        setStorageContext(emptyList())
    }

    private fun setStorageContext(stack: List<String>) {
        if (_storageBrowserState.value.stack == stack) return

        val newPath = stack.lastOrNull()
        _storageBrowserState.update { state ->
            when (newPath) {
                null -> StorageBrowserState()
                else -> state.copy(
                    stack = stack,
                    currentPath = newPath,
                    entries = emptyList(),
                    isLoading = true
                )
            }
        }
        newPath?.let { loadStorageEntries(it) }
    }


    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            // show loading only if still on same path
            _storageBrowserState.update { state ->
                if (state.currentPath == path) state.copy(isLoading = true) else state
            }
            try {
                val appContext = getApplication<Application>()
                val entries = withContext(Dispatchers.IO) {
                    // SINGLE, CONSISTENT CALL
                    FileUtils.listDirectoryEntries(appContext, path)
                }
                _storageBrowserState.update { state ->
                    if (state.currentPath == path) state.copy(entries = entries, isLoading = false) else state
                }
            } catch (e: Exception) {
                _storageBrowserState.update { state ->
                    if (state.currentPath == path) state.copy(entries = emptyList(), isLoading = false) else state
                }
            } finally {
                _storageBrowserState.update { state ->
                    if (state.currentPath == path) state.copy(isLoading = false) else state
                }
            }
        }
    }


    // ===== Storage navigation (single, deduped implementation) =====
    fun openStorage(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun navigateIntoStorage(path: String) {
        val current = _storageBrowserState.value.stack
        if (current.isNotEmpty() && current.last() == path) return
        setStorageContext(current + path)
    }


    // ===== Selection helpers =====
    fun toggleFileSelection(filePath: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (!current.add(filePath)) current.remove(filePath)
        _selectedFiles.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun selectAllFiles(files: List<FileItem>) {
        _selectedFiles.value = files.map { it.path }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
        _isSelectionMode.value = false
    }

    private fun refreshQuickFilter() {
        val current = _quickFilterState.value ?: return
        _quickFilterState.value = buildQuickFilterState(current.filter)
    }

    private fun buildQuickFilterState(filter: QuickFilter): QuickFilterState {
        val files = collectAllFiles()
        val groups: List<QuickFilterGroup> = when (filter) {
            QuickFilter.RECENT -> {
                val sorted = files.sortedByDescending { it.dateModified }
                listOf(QuickFilterGroup(title = null, files = sorted.take(QUICK_FILTER_LIMIT)))
            }

            QuickFilter.LARGE -> {
                val sorted = files.sortedByDescending { it.size }
                listOf(QuickFilterGroup(title = null, files = sorted.take(QUICK_FILTER_LIMIT)))
            }

            QuickFilter.DUPLICATES -> {
                files
                    .groupBy { it.name.lowercase(Locale.getDefault()) to it.size }
                    .values
                    .map { duplicates -> duplicates.distinctBy { it.path } }
                    .filter { it.size > 1 }
                    .map { duplicates ->
                        val header = duplicates.firstOrNull()?.name ?: ""
                        QuickFilterGroup(
                            title = "$header (${duplicates.size})",
                            files = duplicates.sortedByDescending { it.dateModified }
                        )
                    }
                    .sortedByDescending { group -> group.files.firstOrNull()?.size ?: 0L }
            }
        }
        return QuickFilterState(filter = filter, groups = groups)
    }

    private fun collectAllFiles(): List<FileItem> {
        return _categories.value.values.flatMap { data ->
            data.sources.values.flatMap { it.files }
        }
    }

    fun deleteSelectedFiles(): Boolean {
        val selected = _selectedFiles.value
        val cats = _categories.value
        val filesToDelete = buildList {
            cats.values.forEach { data ->
                data.sources.values.forEach { src ->
                    addAll(src.files.filter { it.path in selected })
                }
            }
        }
        val success = FileUtils.deleteFiles(filesToDelete)
        if (success) {
            clearSelection()
            scanFiles()
        }
        return success
    }

    fun getSelectedFileItems(): List<FileItem> {
        val selected = _selectedFiles.value
        val cats = _categories.value
        return buildList {
            cats.values.forEach { data ->
                data.sources.values.forEach { src ->
                    addAll(src.files.filter { it.path in selected })
                }
            }
        }
    }

    fun renameSelectedFile(newName: String): Boolean {
        val selected = _selectedFiles.value
        if (selected.size != 1) return false
        val targetPath = selected.first()

        val cats = _categories.value
        val target = cats.values
            .asSequence()
            .flatMap { it.sources.values.asSequence() }
            .flatMap { it.files.asSequence() }
            .firstOrNull { it.path == targetPath }
            ?: return false

        val ok = FileUtils.renameFile(target, newName)
        if (ok) {
            clearSelection()
            scanFiles()
        }
        return ok
    }

    companion object {
        private const val QUICK_FILTER_LIMIT = 100
    }
}
