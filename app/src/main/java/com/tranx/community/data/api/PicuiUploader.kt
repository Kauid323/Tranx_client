package com.tranx.community.data.api

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.tranx.community.data.model.PicuiTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object PicuiUploader {

    private const val MAX_TOKEN_EXPIRE_SECONDS = 2_626_560

    private val apiService by lazy { PicuiApiService.create() }

    suspend fun uploadImage(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                val fileName = queryDisplayName(context, uri) ?: "tranx_${System.currentTimeMillis()}.jpg"

                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IOException("无法读取图片"))

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

                // 检查是否有保存的 Picui Token
                val picuiToken = prefs.getPicuiToken()?.trim()
                if (picuiToken.isNullOrEmpty()) {
                    return@withContext Result.failure(IOException("请先在设置中配置 Picui Token"))
                }

                val authorization = if (picuiToken.startsWith("Bearer ")) picuiToken else "Bearer $picuiToken"
                
                val temporaryToken = requestUploadToken(authorization)
                    ?: return@withContext Result.failure(IOException("获取上传 Token 失败"))
                val tokenBody = temporaryToken.toRequestBody("text/plain".toMediaType())

                val response = apiService.uploadImage(
                    file = part,
                    token = tokenBody
                )

                if (response.status) {
                    val url = response.data?.links?.url
                    if (!url.isNullOrEmpty()) {
                        Result.success(url)
                    } else {
                        Result.failure(IOException("上传成功但未返回URL"))
                    }
                } else {
                    Result.failure(IOException(response.message ?: "上传失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun requestUploadToken(authorization: String): String? {
        return try {
            val response = apiService.generateUploadToken(
                authorization = authorization,
                request = PicuiTokenRequest(
                    num = 1,
                    seconds = MAX_TOKEN_EXPIRE_SECONDS
                )
            )
            if (response.status) {
                response.data?.tokens?.firstOrNull()?.token
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                name = it.getString(index)
            }
        }
        return name
    }
}

