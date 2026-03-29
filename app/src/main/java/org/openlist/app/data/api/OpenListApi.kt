package org.openlist.app.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.openlist.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface OpenListApi {

    // ============ Auth ============

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<CommonResponse>

    @GET("api/auth/me")
    suspend fun me(): Response<MeResponse>

    // ============ File System ============

    @GET("api/fs/list")
    suspend fun list(
        @Query("path") path: String,
        @Query("password") password: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("refresh") refresh: Boolean = false
    ): Response<FileListResponse>

    @GET("api/fs/get")
    suspend fun getFile(
        @Query("path") path: String,
        @Query("password") password: String? = null,
        @Query("sign") sign: String? = null
    ): Response<FileDetailResponse>

    @POST("api/fs/mkdir")
    suspend fun mkdir(@Body request: MkdirRequest): Response<CommonResponse>

    @POST("api/fs/rename")
    suspend fun rename(@Body request: RenameRequest): Response<CommonResponse>

    @POST("api/fs/remove")
    suspend fun remove(@Body request: DeleteRequest): Response<CommonResponse>

    @POST("api/fs/move")
    suspend fun move(@Body request: MoveRequest): Response<CommonResponse>

    @POST("api/fs/copy")
    suspend fun copy(@Body request: CopyRequest): Response<CommonResponse>

    // ============ Download ============

    @GET("api/fs/download")
    suspend fun getDownloadLink(
        @Query("path") path: String,
        @Query("sign") sign: String? = null
    ): Response<DownloadLinkResponse>

    // ============ Upload ============

    @Multipart
    @POST("api/fs/upload")
    suspend fun upload(
        @Part("path") path: RequestBody,
        @Part("password") password: RequestBody?,
        @Part("replace") replace: RequestBody?,
        @Part("ashima")ashima: RequestBody?,
        @Part file: MultipartBody.Part
    ): Response<CommonResponse>

    // ============ Settings ============

    @GET("api/public/settings")
    suspend fun getSettings(): Response<SettingsResponse>

    // ============ SSO ============

    @GET("api/auth/sso/url")
    suspend fun getSSOUrl(): Response<SSOUrlResponse>

    // ============ Storage ============

    @GET("api/storage/list")
    suspend fun getStorageList(): Response<StorageListResponse>
}
