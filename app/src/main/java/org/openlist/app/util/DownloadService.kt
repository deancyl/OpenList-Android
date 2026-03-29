package org.openlist.app.util

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openlist.app.OpenListApp
import org.openlist.app.R
import org.openlist.app.ui.MainActivity
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadJobs = mutableMapOf<String, Job>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("准备下载..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILENAME) ?: "download"
                val saveDir = intent.getStringExtra(EXTRA_SAVE_DIR) ?: getExternalFilesDir("Download")?.absolutePath ?: ""
                startDownload(url, fileName, saveDir)
            }
            ACTION_CANCEL -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return START_NOT_STICKY
                cancelDownload(taskId)
            }
            ACTION_CANCEL_ALL -> cancelAll()
        }
        return START_STICKY
    }

    private fun startDownload(url: String, fileName: String, saveDir: String) {
        val taskId = "${System.currentTimeMillis()}"
        val savePath = "$saveDir/$fileName"
        val file = File(savePath)

        // Ensure parent directory exists
        file.parentFile?.mkdirs()

        val notification = createNotification("下载中: $fileName", progress = 0)

        downloadJobs[taskId] = scope.launch {
            try {
                updateNotification(taskId, "下载中: $fileName", 0)

                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    updateNotification(taskId, "下载失败: $fileName", -1)
                    return@launch
                }

                val body = response.body ?: run {
                    updateNotification(taskId, "下载失败: $fileName", -1)
                    return@launch
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                FileOutputStream(file).use { fos ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                updateNotification(taskId, "下载中: $fileName", progress)
                            }
                        }
                    }
                }

                updateNotification(taskId, "下载完成: $fileName", 100)
                delay(2000)
                stopSelf()

            } catch (e: Exception) {
                e.printStackTrace()
                updateNotification(taskId, "下载失败: ${e.message}", -1)
                delay(3000)
                stopSelf()
            } finally {
                downloadJobs.remove(taskId)
            }
        }
    }

    private fun cancelDownload(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        if (downloadJobs.isEmpty()) stopSelf()
    }

    private fun cancelAll() {
        scope.cancel()
        stopSelf()
    }

    private fun updateNotification(taskId: String, text: String, progress: Int) {
        val notification = createNotification(text, progress)
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(taskId.hashCode(), notification)
    }

    private fun createNotification(text: String, progress: Int = -1): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, OpenListApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "org.openlist.app.DOWNLOAD_START"
        const val ACTION_CANCEL = "org.openlist.app.DOWNLOAD_CANCEL"
        const val ACTION_CANCEL_ALL = "org.openlist.app.DOWNLOAD_CANCEL_ALL"
        const val EXTRA_URL = "url"
        const val EXTRA_FILENAME = "filename"
        const val EXTRA_SAVE_DIR = "save_dir"
        const val EXTRA_TASK_ID = "task_id"
        const val NOTIFICATION_ID = 1001

        fun startDownload(context: Context, url: String, fileName: String, saveDir: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILENAME, fileName)
                putExtra(EXTRA_SAVE_DIR, saveDir)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
