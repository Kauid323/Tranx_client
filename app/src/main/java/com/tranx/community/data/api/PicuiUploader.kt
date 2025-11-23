package com.tranx.community.data.api

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.tranx.community.TranxApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object PicuiUploader {

    private val apiService by lazy { PicuiApiService.create() }
    private val prefs by lazy { TranxApp.instance.preferencesManager }

    suspend fun uploadImage(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                val fileName = queryDisplayName(context, uri) ?: "tranx_${System.currentTimeMillis()}.jpg"

                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IOException("无法读取图片"))

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

                val token = prefs.getPicuiToken()
                val response = apiService.uploadImage(
                    authorization = token?.let { "Bearer $it" },
                    file = part
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

