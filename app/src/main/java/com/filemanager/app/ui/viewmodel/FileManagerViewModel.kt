package com.filemanager.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemanager.app.data.CategoryData
import com.filemanager.app.data.FileCategory
import com.filemanager.app.data.FileItem
import com.filemanager.app.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

