package org.openlist.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.openlist.app.data.model.*
import org.openlist.app.data.repository.OpenListRepository
import org.openlist.app.data.repository.PreferencesRepository
import org.openlist.app.data.repository.Result
import javax.inject.Inject

data class FileListUiState(
    val files: List<FileItem> = emptyList(),
    val currentPath: String = "/",
    val pathHistory: List<String> = listOf("/"),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedFiles: Set<FileItem> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isGridView: Boolean = false,
    val sortOrder: String = "name",
    val sortAscending: Boolean = true,
    val searchQuery: String = "",
    val filteredFiles: List<FileItem>? = null,
    val passwordRequired: String? = null,
    val readmeContent: String? = null
)

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val repository: OpenListRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileListUiState())
    val uiState: StateFlow<FileListUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        loadRoot()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val gridView = preferencesRepository.getGridView()
            val sortOrder = preferencesRepository.getSortOrder()
            _uiState.update { it.copy(isGridView = gridView, sortOrder = sortOrder) }
        }
    }

    fun loadRoot() {
        navigateTo("/")
    }

    fun navigateTo(path: String, password: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    passwordRequired = null,
                    readmeContent = null,
                    filteredFiles = null,
                    searchQuery = ""
                )
            }

            when (val result = repository.listFiles(path, password)) {
                is Result.Success -> {
                    val data = result.data
                    val sorted = sortFiles(data.content ?: emptyList())
                    val currentHistory = _uiState.value.pathHistory
                    val currentIndex = currentHistory.indexOf(path)
                    val newHistory: List<String> = if (currentIndex >= 0) {
                        currentHistory.subList(0, currentIndex + 1)
                    } else {
                        currentHistory + path
                    }

                    _uiState.update {
                        it.copy(
                            files = sorted,
                            currentPath = path,
                            pathHistory = newHistory,
                            isLoading = false,
                            isRefreshing = false,
                            readmeContent = data.readme
                        )
                    }
                }
                is Result.Error -> {
                    if (result.code == 401) {
                        _uiState.update {
                            it.copy(isLoading = false, isRefreshing = false, passwordRequired = path)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, isRefreshing = false, error = result.message)
                        }
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        navigateTo(_uiState.value.currentPath)
    }

    fun navigateBack(): Boolean {
        val history = _uiState.value.pathHistory
        if (history.size <= 1) return false
        val parentPath = history[history.size - 2]
        navigateTo(parentPath)
        return true
    }

    fun openFolder(file: FileItem) {
        if (!file.is_dir) return
        val newPath = if (file.path.isNullOrEmpty()) {
            "${_uiState.value.currentPath}/${file.name}".replace("//", "/")
        } else {
            file.path + "/" + file.name
        }
        navigateTo(newPath)
    }

    fun onPasswordEntered(password: String) {
        _uiState.update { it.copy(passwordRequired = null) }
        val path = _uiState.value.passwordRequired ?: "/"
        navigateTo(path, password)
    }

    // Selection
    fun toggleSelection(file: FileItem) {
        _uiState.update { state ->
            val newSelected = state.selectedFiles.toMutableSet()
            if (newSelected.contains(file)) newSelected.remove(file) else newSelected.add(file)
            state.copy(
                selectedFiles = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFiles = emptySet(), isSelectionMode = false) }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedFiles = it.files.toSet(), isSelectionMode = true) }
    }

    // Search
    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) null
            else state.files.filter { it.name.contains(query, ignoreCase = true) }
            state.copy(searchQuery = query, filteredFiles = filtered)
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", filteredFiles = null) }
    }

    // View mode
    fun toggleViewMode() {
        viewModelScope.launch {
            val newGridView = !_uiState.value.isGridView
            preferencesRepository.setGridView(newGridView)
            _uiState.update { it.copy(isGridView = newGridView) }
        }
    }

    // Sort
    fun toggleSortOrder(order: String) {
        viewModelScope.launch {
            val current = _uiState.value
            val newAscending = if (current.sortOrder == order) !current.sortAscending else true
            preferencesRepository.setSortOrder(order)
            _uiState.update { it.copy(sortOrder = order, sortAscending = newAscending) }
            resortFiles()
        }
    }

    private fun resortFiles() {
        _uiState.update { it.copy(files = sortFiles(it.files)) }
    }

    private fun sortFiles(files: List<FileItem>): List<FileItem> {
        val state = _uiState.value
        val dirs = files.filter { it.is_dir }
        val others = files.filter { !it.is_dir }

        val sortedDirs = when (state.sortOrder) {
            "name" -> if (state.sortAscending) dirs.sortedBy { it.name.lowercase() } else dirs.sortedByDescending { it.name.lowercase() }
            "size" -> if (state.sortAscending) dirs.sortedBy { it.size } else dirs.sortedByDescending { it.size }
            "modified" -> if (state.sortAscending) dirs.sortedBy { it.modified } else dirs.sortedByDescending { it.modified }
            else -> dirs
        }

        val sortedOthers = when (state.sortOrder) {
            "name" -> if (state.sortAscending) others.sortedBy { it.name.lowercase() } else others.sortedByDescending { it.name.lowercase() }
            "size" -> if (state.sortAscending) others.sortedBy { it.size } else others.sortedByDescending { it.size }
            "modified" -> if (state.sortAscending) others.sortedBy { it.modified } else others.sortedByDescending { it.modified }
            else -> others
        }

        return sortedDirs + sortedOthers
    }

    // Delete
    fun deleteSelected(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val items = _uiState.value.selectedFiles.map {
                DeleteItem(
                    path = it.path ?: "${_uiState.value.currentPath}/${it.name}",
                    name = it.name,
                    is_dir = it.is_dir
                )
            }
            when (repository.delete(items)) {
                is Result.Success -> {
                    clearSelection()
                    refresh()
                    onResult(true)
                }
                else -> onResult(false)
            }
        }
    }

    // Rename
    fun rename(oldName: String, newName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val path = _uiState.value.currentPath
            when (repository.rename(path, oldName, newName)) {
                is Result.Success -> {
                    refresh()
                    onResult(true)
                }
                else -> onResult(false)
            }
        }
    }

    // Mkdir
    fun createFolder(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (repository.mkdir(_uiState.value.currentPath, name)) {
                is Result.Success -> {
                    refresh()
                    onResult(true)
                }
                else -> onResult(false)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    val displayedFiles: List<FileItem>
        get() = _uiState.value.filteredFiles ?: _uiState.value.files
}
