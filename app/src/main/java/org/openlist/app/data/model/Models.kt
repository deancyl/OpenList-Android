package org.openlist.app.data.model

import com.google.gson.annotations.SerializedName

// ============ Auth ============

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val code: Int,
    val message: String,
    val data: TokenData?
)

data class TokenData(
    val token: String?
)

// ============ File System ============

data class FileListResponse(
    val code: Int,
    val message: String,
    val data: FileListData?
)

data class FileListData(
    val content: List<FileItem>?,
    val total: Int,
    val page: Int,
    val per_page: Int,
    val readme: String?,
    val write: Boolean?
)

data class FileItem(
    val name: String,
    val size: Long,
    val is_dir: Boolean,
    @SerializedName("sign")
    val sign: String?,
    val thumb: String?,
    val type: Int,          // 1=image, 2=text, 3=video, 4=audio, 5=pdf, etc.
    val modified: String?,
    val created: String?,
    val path: String?,
    val hash: String?,
    val provider: String?,
    val raw: Map<String, Any>?
) {
    val isImage: Boolean get() = type == 1
    val isVideo: Boolean get() = type == 3
    val isAudio: Boolean get() = type == 4
    val isPdf: Boolean get() = type == 5

    fun getDisplayType(): String = when {
        is_dir -> "文件夹"
        isImage -> "图片"
        isVideo -> "视频"
        isAudio -> "音频"
        isPdf -> "PDF"
        else -> "文件"
    }
}

// ============ Single File ============

data class FileDetailResponse(
    val code: Int,
    val message: String,
    val data: FileDetail?
)

data class FileDetail(
    val name: String,
    val size: Long,
    val is_dir: Boolean,
    val modified: String?,
    val created: String?,
    val path: String?,
    val thumb: String?,
    val type: Int,
    val sign: String?,
    val provider: String?,
    val hash: String?
)

// ============ Storage / Settings ============

data class StorageListResponse(
    val code: Int,
    val message: String,
    val data: List<StorageItem>?
)

data class StorageItem(
    val id: Int,
    val name: String,
    val driver: String,
    val addition: String?,
    val status: String?,
    @SerializedName("mounted")
    val mounted: List<String>?
)

data class SettingsResponse(
    val code: Int,
    val message: String,
    val data: SettingsData?
)

data class SettingsData(
    val site_title: String?,
    val version: String?,
    val allow_signup: Boolean?,
    val init: Boolean?,
    val maintance: Boolean?
)

// ============ Mkdir / Upload ============

data class MkdirRequest(
    val path: String,
    val name: String
)

data class MkdirResponse(
    val code: Int,
    val message: String
)

// ============ Rename ============

data class RenameRequest(
    val path: String,
    val name: String,
    @SerializedName("new_name")
    val newName: String
)

data class RenameResponse(
    val code: Int,
    val message: String
)

// ============ Delete ============

data class DeleteRequest(
    val items: List<DeleteItem>
)

data class DeleteItem(
    val path: String,
    val name: String,
    val is_dir: Boolean
)

data class DeleteResponse(
    val code: Int,
    val message: String
)

// ============ Move / Copy ============

data class MoveRequest(
    val src_dir: String,
    val dst_dir: String,
    val items: List<RenameItem>
)

data class CopyRequest(
    val src_dir: String,
    val dst_dir: String,
    val items: List<RenameItem>
)

data class RenameItem(
    val path: String,
    val name: String,
    @SerializedName("new_name")
    val newName: String?
)

// ============ Download ============

data class DownloadLinkResponse(
    val code: Int,
    val message: String,
    val data: DownloadLink?
)

data class DownloadLink(
    val name: String?,
    val size: Long?,
    val url: String?
)

// ============ SSO / OAuth ============

data class SSOUrlResponse(
    val code: Int,
    val message: String,
    val data: SSOUrlData?
)

data class SSOUrlData(
    val url: String?
)

// ============ Me ============

data class MeResponse(
    val code: Int,
    val message: String,
    val data: MeData?
)

data class MeData(
    val username: String?,
    val nickame: String?,
    val email: String?,
    val group: String?,
    val role: String?
)

// ============ Common ============

data class CommonResponse(
    val code: Int,
    val message: String
) {
    val isSuccess: Boolean get() = code == 0
}

data class ErrorResponse(
    val code: Int,
    val message: String
)

// ============ Pagination ============

data class Pagination(
    val page: Int = 1,
    val perPage: Int = 100,
    val refresh: Boolean = false
)
