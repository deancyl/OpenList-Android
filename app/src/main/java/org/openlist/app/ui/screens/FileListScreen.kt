package org.openlist.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import org.openlist.app.data.model.FileItem
import org.openlist.app.ui.components.*
import org.openlist.app.ui.theme.*
import org.openlist.app.ui.viewmodels.FileListViewModel
import org.openlist.app.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    onNavigateToPreview: (FileItem) -> Unit,
    onLogout: () -> Unit,
    viewModel: FileListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    BackHandler(enabled = viewModel.uiState.value.currentPath != "/") {
        if (!viewModel.navigateBack()) onLogout()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("搜索文件...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                            )
                        )
                    } else {
                        Column {
                            Text(
                                text = uiState.currentPath.ifEmpty { "/" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.selectedFiles.isNotEmpty()) {
                                Text(
                                    text = "已选择 ${uiState.selectedFiles.size} 项",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    } else {
                        IconButton(onClick = {
                            if (!viewModel.navigateBack()) onLogout()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    // Search
                    IconButton(onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) viewModel.clearSearch()
                    }) {
                        Icon(
                            if (showSearchBar) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }

                    // View toggle
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "切换视图"
                        )
                    }

                    // Sort
                    if (!uiState.isSelectionMode) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "排序")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortMenuItems(
                                    currentSort = uiState.sortOrder,
                                    ascending = uiState.sortAscending,
                                    onSelect = {
                                        viewModel.toggleSortOrder(it)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Settings / More
                    if (!uiState.isSelectionMode) {
                        Box {
                            IconButton(onClick = { showSettingsMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("新建文件夹") },
                                    onClick = {
                                        showSettingsMenu = false
                                        showNewFolderDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.CreateNewFolder, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("刷新") },
                                    onClick = {
                                        showSettingsMenu = false
                                        viewModel.refresh()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, null)
                                    }
                                )
                                if (uiState.isSelectionMode) {
                                    DropdownMenuItem(
                                        text = { Text("全选") },
                                        onClick = {
                                            showSettingsMenu = false
                                            viewModel.selectAll()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.SelectAll, null)
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("设置") },
                                    onClick = { showSettingsMenu = false },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, null)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode && uiState.selectedFiles.isEmpty()) {
                FloatingActionButton(
                    onClick = { showNewFolderDialog = true }
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "新建文件夹")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.files.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null && uiState.files.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("重试")
                        }
                    }
                }

                viewModel.displayedFiles.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "文件夹为空",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                else -> {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(uiState.isRefreshing),
                        onRefresh = { viewModel.refresh() }
                    ) {
                        if (uiState.isGridView) {
                            FileGrid(
                                files = viewModel.displayedFiles,
                                selectedFiles = uiState.selectedFiles,
                                onFileClick = { file ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(file)
                                    } else if (file.is_dir) {
                                        viewModel.openFolder(file)
                                    } else {
                                        onNavigateToPreview(file)
                                    }
                                },
                                onFileLongClick = { viewModel.toggleSelection(it) }
                            )
                        } else {
                            FileList(
                                files = viewModel.displayedFiles,
                                selectedFiles = uiState.selectedFiles,
                                onFileClick = { file ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(file)
                                    } else if (file.is_dir) {
                                        viewModel.openFolder(file)
                                    } else {
                                        onNavigateToPreview(file)
                                    }
                                },
                                onFileLongClick = { viewModel.toggleSelection(it) }
                            )
                        }
                    }
                }
            }

            // README preview at top
            uiState.readmeContent?.let { readme ->
                if (readme.isNotBlank() && !uiState.isSelectionMode) {
                    ReadmeBanner(
                        content = readme,
                        onDismiss = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    // Dialogs
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name) { success ->
                    showNewFolderDialog = false
                }
            }
        )
    }

    showRenameDialog?.let { file ->
        RenameDialog(
            oldName = file.name,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newName ->
                viewModel.rename(file.name, newName) { showRenameDialog = null }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${uiState.selectedFiles.size} 项吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected { showDeleteConfirm = false }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                showPasswordDialog = false
                viewModel.onPasswordEntered(password)
            }
        )
    }
}
