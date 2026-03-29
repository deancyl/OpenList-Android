package org.openlist.app.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(1024.0, digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }

    fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        return try {
            val timestamp = dateString.toLong() * 1000
            dateFormat.format(Date(timestamp))
        } catch (e: Exception) {
            dateString
        }
    }

    fun getFileExtension(name: String): String {
        val lastDot = name.lastIndexOf('.')
        return if (lastDot >= 0) name.substring(lastDot + 1).lowercase() else ""
    }

    fun getMimeType(name: String): String {
        val ext = getFileExtension(name)
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"
            "mp4", "avi", "mov", "mkv", "flv", "wmv" -> "video/*"
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> "audio/*"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt", "md" -> "text/plain"
            "zip", "rar", "7z", "tar", "gz" -> "application/zip"
            "html", "htm" -> "text/html"
            "css", "js", "json", "xml" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    fun isPreviewable(name: String): Boolean {
        val ext = getFileExtension(name)
        return ext in listOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "mp4", "avi", "mov", "mkv", "flv",
            "mp3", "wav", "flac", "aac", "ogg", "m4a",
            "pdf", "txt", "md", "html", "css", "js", "json"
        )
    }

    fun getParentPath(path: String): String {
        val trimmed = path.trimEnd('/')
        val lastSlash = trimmed.lastIndexOf('/')
        return if (lastSlash >= 0) trimmed.substring(0, lastSlash) else "/"
    }

    fun normalizePath(path: String): String {
        return if (path.isEmpty()) "/" else if (path.startsWith("/")) path else "/$path"
    }
}
