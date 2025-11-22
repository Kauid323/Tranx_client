package com.tranx.community.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    private var apiService: ApiService? = null
    private var currentBaseUrl: String = ""

    fun initialize(baseUrl: String) {
        if (currentBaseUrl != baseUrl || apiService == null) {
            currentBaseUrl = baseUrl
            
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)

            // 如果是HTTP，添加信任所有证书（仅用于开发）
            if (baseUrl.startsWith("http://")) {
                try {
                    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })

                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                    
                    clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    clientBuilder.hostnameVerifier { _, _ -> true }
                } catch (e: Exception) {
                    println("SSL配置失败: ${e.message}")
                }
            }

            val client = clientBuilder.build()

            // 确保baseUrl以斜杠结尾
            val finalBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val retrofit = Retrofit.Builder()
                .baseUrl(finalBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
            
            // 打印调试信息
            println("RetrofitClient initialized with URL: $finalBaseUrl")
        }
    }

    fun getApiService(): ApiService {
        return apiService ?: throw IllegalStateException("RetrofitClient未初始化，请先调用initialize()方法")
    }

    fun isInitialized(): Boolean {
        return apiService != null
    }
    
    fun getCurrentBaseUrl(): String {
        return currentBaseUrl
    }
}

