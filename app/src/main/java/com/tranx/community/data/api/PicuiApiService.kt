package com.tranx.community.data.api

import com.tranx.community.data.model.PicuiUploadResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PicuiApiService {

    @Multipart
    @POST("/upload")
    suspend fun uploadImage(
        @Header("Authorization") authorization: String?,
        @Header("Accept") accept: String = "application/json",
        @Part file: MultipartBody.Part
    ): PicuiUploadResponse

    companion object {
        private const val BASE_URL = "https://picui.cn/api/v1/"

        fun create(): PicuiApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PicuiApiService::class.java)
        }
    }
}

