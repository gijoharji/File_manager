package com.filemanager.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemanager.app.data.CategoryData
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.FileItem
import com.filemanager.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _categories = MutableStateFlow<Map<FileCategory, CategoryData>>(emptyMap())
    val categories: StateFlow<Map<FileCategory, CategoryData>> = _categories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _storageBrowserState = MutableStateFlow(StorageBrowserState())
    val storageBrowserState: StateFlow<StorageBrowserState> = _storageBrowserState.asStateFlow()

    init {
        scanFiles()
    }

    init {
        scanFiles()
    }
    fun scanFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val scannedCategories = FileUtils.scanFiles(getApplication())
                _categories.value = scannedCategories
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

    fun showStorageRoot(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun navigateIntoStorage(path: String) {
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
        if (_storageBrowserState.value.stack == stack) {
            return
        }

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
            _storageBrowserState.update { state ->
                if (state.currentPath == path) state.copy(isLoading = true) else state
            }
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
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

    fun showStorageRoot(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun navigateIntoStorage(path: String) {
        val currentStack = _storageNavigationStack.value
        if (currentStack.isNotEmpty() && currentStack.last() == path) {
            return
        }

        setStorageContext(currentStack + path)
    }

    fun navigateStorageBack(): Boolean {
        val stack = _storageNavigationStack.value
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
        _storageNavigationStack.value = stack

        val newPath = stack.lastOrNull()
        _currentStoragePath.value = newPath

        if (newPath == null) {
            _storageEntries.value = emptyList()
            _isStorageLoading.value = false
            return
        }

        loadStorageEntries(newPath)
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun enterStorageDirectory(path: String) {
        val currentStack = _storageNavigationStack.value
        if (currentStack.isNotEmpty() && currentStack.last() == path) {
            return
        }

        setStorageContext(currentStack + path)
    }

    fun handleStorageBack(): Boolean {
        val stack = _storageNavigationStack.value
        return when {
            stack.size > 1 -> {
                setStorageContext(stack.dropLast(1))
                true
            }

            stack.isNotEmpty() -> {
                dismissStorageBrowser()
                true
            }

            else -> false
        }
    }

    fun dismissStorageBrowser() {
        clearSelection()
        setStorageContext(emptyList())
    }

    private fun setStorageContext(stack: List<String>) {
        _storageNavigationStack.value = stack

        val newPath = stack.lastOrNull()
        _currentStoragePath.value = newPath

        if (newPath == null) {
            _storageEntries.value = emptyList()
            _isStorageLoading.value = false
            return
        }

        loadStorageEntries(newPath)
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun navigateIntoStorage(path: String) {
        val currentStack = _storageNavigationStack.value
        if (currentStack.isNotEmpty() && currentStack.last() == path) {
            return
        }

        setStorageContext(currentStack + path)
    }

    fun handleStorageBack(): Boolean {
        val stack = _storageNavigationStack.value
        return when {
            stack.size > 1 -> {
                setStorageContext(stack.dropLast(1))
                true
            }

            stack.isNotEmpty() -> {
                dismissStorageBrowser()
                true
            }

            else -> false
        }
    }

    fun dismissStorageBrowser() {
        clearSelection()
        setStorageContext(emptyList())
    }

    private fun setStorageContext(stack: List<String>) {
        _storageNavigationStack.value = stack

        val newPath = stack.lastOrNull()
        _currentStoragePath.value = newPath

        if (newPath == null) {
            _storageEntries.value = emptyList()
            _isStorageLoading.value = false
            return
        }

        loadStorageEntries(newPath)
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun navigateIntoStorage(path: String) {
        val currentStack = _storageNavigationStack.value
        if (currentStack.isNotEmpty() && currentStack.last() == path) {
            return
        }

        setStorageContext(currentStack + path)
    }

    fun navigateUpStorage(): Boolean {
        val stack = _storageNavigationStack.value
        if (stack.size <= 1) {
            return false
        }

        setStorageContext(stack.dropLast(1))
        return true
    }

    fun closeStorageBrowser() {
        clearSelection()
        setStorageContext(emptyList())
    }

    private fun setStorageContext(stack: List<String>) {
        _storageNavigationStack.value = stack

        val newPath = stack.lastOrNull()
        _currentStoragePath.value = newPath

        if (newPath == null) {
            _storageEntries.value = emptyList()
            _isStorageLoading.value = false
            return
        }

        loadStorageEntries(newPath)
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun navigateIntoStorage(path: String) {
        val currentStack = _storageNavigationStack.value
        if (currentStack.isNotEmpty() && currentStack.last() == path) {
            return
        }

        setStorageContext(currentStack + path)
    }

    fun popStorageLevel(): Boolean {
        val stack = _storageNavigationStack.value
        if (stack.size <= 1) {
            return false
        }

        setStorageContext(stack.dropLast(1))
        return true
    }

    fun closeStorageBrowser() {
        clearSelection()
        setStorageContext(emptyList())
    }

    private fun setStorageContext(stack: List<String>) {
        _storageNavigationStack.value = stack

        val newPath = stack.lastOrNull()
        _currentStoragePath.value = newPath

        if (newPath == null) {
            _storageEntries.value = emptyList()
            _isStorageLoading.value = false
            return
        }

        loadStorageEntries(newPath)
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        clearSelection()
        _selectedCategory.value = null
        setStorageContext(listOf(path))
    }

    fun navigateIntoStorage(path: String) {
        val currentStack = _storageNavigationStack.value
        if (currentStack.isNotEmpty() && currentStack.last() == path) {
            return
        }

        setStorageContext(currentStack + path)
    }

    fun navigateUpStorage(): Boolean {
        val stack = _storageNavigationStack.value
        if (stack.size <= 1) {
            return false
        }

        setStorageContext(stack.dropLast(1))
        return true
    }

    fun closeStorageBrowser() {
        clearSelection()
        setStorageContext(emptyList())
    }

    private fun setStorageContext(stack: List<String>) {
        _storageNavigationStack.value = stack

        val newPath = stack.lastOrNull()
        _currentStoragePath.value = newPath

        if (newPath == null) {
            _storageEntries.value = emptyList()
            _isStorageLoading.value = false
            return
        }

        loadStorageEntries(newPath)
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        _selectedCategory.value = null
        _storageNavigationStack.value = listOf(path)
        _currentStoragePath.value = path
        clearSelection()
        loadStorageEntries(path)
    }

    /**
     * Backwards compatible alias for callers that previously referenced a
     * generic navigateStorage entry point. This avoids unresolved references
     * while deferring to the newer navigation helper that updates the stack.
     */
    fun navigateStorage(path: String) {
        navigateIntoStorage(path)
    }

    fun navigateIntoStorage(path: String) {
        val stack = _storageNavigationStack.value + path
        _storageNavigationStack.value = stack
        _currentStoragePath.value = path
        loadStorageEntries(path)
    }

    fun navigateUpStorage(): Boolean {
        val stack = _storageNavigationStack.value
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            val newPath = newStack.last()
            _storageNavigationStack.value = newStack
            _currentStoragePath.value = newPath
            loadStorageEntries(newPath)
            return true
        }
        return false
    }

    /**
     * Matches older call sites that expected a navigate-up style helper.
     * Returns whether the navigation event was consumed.
     */
    fun navigateStorageUp(): Boolean = navigateUpStorage()

    fun closeStorageBrowser() {
        _storageNavigationStack.value = emptyList()
        _currentStoragePath.value = null
        _storageEntries.value = emptyList()
        _isStorageLoading.value = false
    }

    fun closeStorage() {
        closeStorageBrowser()
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        _selectedCategory.value = null
        _storageNavigationStack.value = listOf(path)
        _currentStoragePath.value = path
        clearSelection()
        loadStorageEntries(path)
    }

    fun navigateIntoStorage(path: String) {
        val stack = _storageNavigationStack.value + path
        _storageNavigationStack.value = stack
        _currentStoragePath.value = path
        loadStorageEntries(path)
    }

    fun navigateUpStorage(): Boolean {
        val stack = _storageNavigationStack.value
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            val newPath = newStack.last()
            _storageNavigationStack.value = newStack
            _currentStoragePath.value = newPath
            loadStorageEntries(newPath)
            return true
        }
        return false
    }

    fun closeStorageBrowser() {
        _storageNavigationStack.value = emptyList()
        _currentStoragePath.value = null
        _storageEntries.value = emptyList()
        _isStorageLoading.value = false
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun openStorage(path: String) {
        _selectedCategory.value = null
        _storageNavigationStack.value = listOf(path)
        _currentStoragePath.value = path
        clearSelection()
        loadStorageEntries(path)
    }

    fun navigateIntoStorage(path: String) {
        val stack = _storageNavigationStack.value + path
        _storageNavigationStack.value = stack
        _currentStoragePath.value = path
        loadStorageEntries(path)
    }

    fun navigateUpStorage(): Boolean {
        val stack = _storageNavigationStack.value
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            val newPath = newStack.last()
            _storageNavigationStack.value = newStack
            _currentStoragePath.value = newPath
            loadStorageEntries(newPath)
            return true
        }
        return false
    }

    fun closeStorageBrowser() {
        _storageNavigationStack.value = emptyList()
        _currentStoragePath.value = null
        _storageEntries.value = emptyList()
        _isStorageLoading.value = false
    }

    private fun loadStorageEntries(path: String) {
        viewModelScope.launch {
            _isStorageLoading.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    FileUtils.listDirectoryEntries(path)
                }
                _storageEntries.value = entries
            } catch (e: Exception) {
                _storageEntries.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun toggleFileSelection(filePath: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(filePath)) {
            current.remove(filePath)
        } else {
            current.add(filePath)
        }
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

    fun deleteSelectedFiles(): Boolean {
        val selected = _selectedFiles.value
        val categories = _categories.value
        val filesToDelete = mutableListOf<FileItem>()
        
        categories.values.forEach { categoryData ->
            categoryData.sources.values.forEach { source ->
                filesToDelete.addAll(source.files.filter { it.path in selected })
            }
        }
        
        val success = FileUtils.deleteFiles(filesToDelete)
        if (success) {
            clearSelection()
            scanFiles() // Refresh
        }
        return success
    }

    fun getSelectedFileItems(): List<FileItem> {
        val selected = _selectedFiles.value
        val categories = _categories.value
        val selectedFiles = mutableListOf<FileItem>()

        categories.values.forEach { categoryData ->
            categoryData.sources.values.forEach { source ->
                selectedFiles.addAll(source.files.filter { it.path in selected })
            }
        }

        return selectedFiles
    }

    fun renameSelectedFile(newName: String): Boolean {
        val selected = _selectedFiles.value
        if (selected.size != 1) return false

        val filePath = selected.first()
        val categories = _categories.value
        val targetFile = categories.values
            .asSequence()
            .flatMap { it.sources.values.asSequence() }
            .flatMap { it.files.asSequence() }
            .firstOrNull { it.path == filePath }
            ?: return false

        val success = FileUtils.renameFile(targetFile, newName)
        if (success) {
            clearSelection()
            scanFiles()
        }
        return success
    }
}

