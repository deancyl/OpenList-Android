package org.openlist.app.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openlist.app.data.model.FileItem
import org.openlist.app.util.DownloadService
import org.openlist.app.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    file: FileItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Zoom for images
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.name,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = FileUtils.formatFileSize(file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Download
                    IconButton(onClick = {
                        downloadFile(context, file)
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "下载")
                    }
                    // Share
                    IconButton(onClick = {
                        shareFile(context, file)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                    // Info
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(Icons.Default.Info, contentDescription = "详情")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                file.isImage -> {
                    ImagePreview(
                        file = file,
                        isLoading = isLoading,
                        onLoadingChange = { isLoading = it },
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        onTransform = { zoom, panX, panY ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offsetX += panX
                            offsetY += panY
                        },
                        onReset = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    )
                }

                file.isVideo -> {
                    VideoPreview(
                        file = file,
                        onError = { error = it }
                    )
                }

                file.isPdf -> {
                    PdfPreview(
                        file = file,
                        onLoadingChange = { isLoading = it },
                        onError = { error = it }
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂不支持预览此文件类型",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { downloadFile(context, file) }) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("下载到本地")
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            error?.let {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            }

            // Info panel
            if (showInfo) {
                FileInfoPanel(
                    file = file,
                    onDismiss = { showInfo = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun ImagePreview(
    file: FileItem,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onTransform: (zoom: Float, panX: Float, panY: Float) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(zoom, pan.x, pan.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file.thumb ?: file.raw?.get("url") ?: file.sign)
                .crossfade(true)
                .listener(
                    onStart = { onLoadingChange(true) },
                    onSuccess = { _, _ -> onLoadingChange(false) },
                    onError = { _, _ -> onLoadingChange(false) }
                )
                .build(),
            contentDescription = file.name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )

        // Reset zoom button
        if (scale > 1f) {
            FloatingActionButton(
                onClick = onReset,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Icon(Icons.Default.ZoomOutMap, "重置缩放")
            }
        }
    }
}

@Composable
fun VideoPreview(
    file: FileItem,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(file.sign) {
        file.sign?.let { sign ->
            val mediaItem = MediaItem.fromUri(sign)
            player.setMediaItem(mediaItem)
            player.prepare()
        }
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PdfPreview(
    file: FileItem,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current

    // For PDF, we use the system PDF viewer via intent
    LaunchedEffect(file.sign) {
        onLoadingChange(false)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("PDF 预览需要下载后查看", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { downloadFile(context, file) }) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("下载 PDF")
        }
    }
}

@Composable
fun FileInfoPanel(
    file: FileItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .width(280.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "文件信息",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("名称", file.name)
            InfoRow("大小", FileUtils.formatFileSize(file.size))
            InfoRow("类型", if (file.is_dir) "文件夹" else FileUtils.getFileExtension(file.name).uppercase())
            file.modified?.let { InfoRow("修改时间", FileUtils.formatDate(it)) }
            file.provider?.let { InfoRow("存储", it) }
            file.hash?.let { InfoRow("Hash", it.take(16) + "...") }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}

private fun downloadFile(context: Context, file: FileItem) {
    val url = file.sign ?: return
    val saveDir = context.getExternalFilesDir("Download")?.absolutePath ?: return
    DownloadService.startDownload(context, url, file.name, saveDir)
    Toast.makeText(context, "开始下载: ${file.name}", Toast.LENGTH_SHORT).show()
}

private fun shareFile(context: Context, file: FileItem) {
    val url = file.sign ?: return
    val shareIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, "$url\n${file.name}")
        type = "text/plain"
    }
    context.startActivity(android.content.Intent.createChooser(shareIntent, "分享文件"))
}
