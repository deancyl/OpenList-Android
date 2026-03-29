package org.openlist.app.data.repository

import org.openlist.app.data.api.OpenListApi
import org.openlist.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val code: Int = -1, val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

@Singleton
class OpenListRepository @Inject constructor(
    private val api: OpenListApi,
    private val preferencesRepository: PreferencesRepository
) {

    // ============ Auth ============

    suspend fun login(serverUrl: String, username: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (body.code == 0) {
                        preferencesRepository.setServerUrl(serverUrl)
                        preferencesRepository.setUsername(username)
                        preferencesRepository.setPassword(password)
                        body.data?.token?.let { preferencesRepository.setToken(it) }
                        preferencesRepository.setLoggedIn(true)
                    }
                    Result.Success(body)
                } ?: Result.Error(message = "Empty response")
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun logout(): Result<CommonResponse> {
        return try {
            val response = api.logout()
            if (response.isSuccessful) {
                preferencesRepository.clearAll()
                Result.Success(response.body() ?: CommonResponse(0, "Logged out"))
            } else {
                preferencesRepository.clearAll()
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            preferencesRepository.clearAll()
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun getMe(): Result<MeResponse> {
        return try {
            val response = api.me()
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    // ============ File System ============

    suspend fun listFiles(
        path: String,
        password: String? = null,
        page: Int = 1,
        perPage: Int = 100,
        refresh: Boolean = false
    ): Result<FileListData> {
        return try {
            val response = api.list(path, password, page, perPage, refresh)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (body.code == 0) {
                        Result.Success(body.data ?: FileListData(null, 0, page, perPage, null, null))
                    } else {
                        Result.Error(body.code, body.message)
                    }
                } ?: Result.Error(message = "Empty response")
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun getFileDetail(path: String, password: String? = null, sign: String? = null): Result<FileDetail> {
        return try {
            val response = api.getFile(path, password, sign)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (body.code == 0) {
                        body.data?.let { Result.Success(it) }
                            ?: Result.Error(message = "No file data")
                    } else {
                        Result.Error(body.code, body.message)
                    }
                } ?: Result.Error(message = "Empty response")
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun mkdir(path: String, name: String): Result<CommonResponse> {
        return try {
            val response = api.mkdir(MkdirRequest(path, name))
            if (response.isSuccessful) {
                Result.Success(response.body() ?: CommonResponse(0, "Success"))
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun rename(path: String, name: String, newName: String): Result<CommonResponse> {
        return try {
            val response = api.rename(RenameRequest(path, name, newName))
            if (response.isSuccessful) {
                Result.Success(response.body() ?: CommonResponse(0, "Success"))
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun delete(items: List<DeleteItem>): Result<CommonResponse> {
        return try {
            val response = api.remove(DeleteRequest(items))
            if (response.isSuccessful) {
                Result.Success(response.body() ?: CommonResponse(0, "Success"))
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun move(srcDir: String, dstDir: String, items: List<RenameItem>): Result<CommonResponse> {
        return try {
            val response = api.move(MoveRequest(srcDir, dstDir, items))
            if (response.isSuccessful) {
                Result.Success(response.body() ?: CommonResponse(0, "Success"))
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    suspend fun copy(srcDir: String, dstDir: String, items: List<RenameItem>): Result<CommonResponse> {
        return try {
            val response = api.copy(CopyRequest(srcDir, dstDir, items))
            if (response.isSuccessful) {
                Result.Success(response.body() ?: CommonResponse(0, "Success"))
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    // ============ Download ============

    suspend fun getDownloadLink(path: String, sign: String? = null): Result<DownloadLink> {
        return try {
            val response = api.getDownloadLink(path, sign)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (body.code == 0) {
                        body.data?.let { Result.Success(it) }
                            ?: Result.Error(message = "No download link")
                    } else {
                        Result.Error(body.code, body.message)
                    }
                } ?: Result.Error(message = "Empty response")
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }

    // ============ Settings ============

    suspend fun getSettings(): Result<SettingsData> {
        return try {
            val response = api.getSettings()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (body.code == 0) {
                        body.data?.let { Result.Success(it) }
                            ?: Result.Error(message = "No settings data")
                    } else {
                        Result.Error(body.code, body.message)
                    }
                } ?: Result.Error(message = "Empty response")
            } else {
                Result.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Network error")
        }
    }
}
