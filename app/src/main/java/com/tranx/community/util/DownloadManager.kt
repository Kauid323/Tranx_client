package com.tranx.community.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val userAgent: String?,
    val totalLength: Long,
    var downloadedLength: Long = 0,
    var status: DownloadStatus = DownloadStatus.PENDING,
    var filePath: String? = null
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, ERROR
}

class DownloadManager(private val context: Context) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    
    private val _tasks = mutableListOf<DownloadTask>()
    val tasks: List<DownloadTask> get() = _tasks

    init {
        loadTasks()
    }

    private fun loadTasks() {
        val json = prefs.getString("tasks", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<DownloadTask>>() {}.type
            val savedTasks: MutableList<DownloadTask> = gson.fromJson(json, type)
            _tasks.addAll(savedTasks)
            // Ensure status is not DOWNLOADING after restart
            _tasks.forEach { if (it.status == DownloadStatus.DOWNLOADING) it.status = DownloadStatus.PAUSED }
        }
    }

    private fun saveTasks() {
        prefs.edit().putString("tasks", gson.toJson(_tasks)).apply()
    }

    fun enqueue(url: String, fileName: String, userAgent: String?, contentLength: Long) {
        val id = java.util.UUID.randomUUID().toString()
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDir, fileName)
        
        val task = DownloadTask(
            id = id,
            url = url,
            fileName = fileName,
            userAgent = userAgent,
            totalLength = contentLength,
            filePath = file.absolutePath
        )
        _tasks.add(task)
        saveTasks()
        startDownload(task)
    }

    fun startDownload(task: DownloadTask) {
        if (task.status == DownloadStatus.COMPLETED) return
        if (activeJobs.containsKey(task.id)) return

        val job = scope.launch {
            try {
                task.status = DownloadStatus.DOWNLOADING
                saveTasks()
                
                val file = File(task.filePath!!)
                val connection = URL(task.url).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", task.userAgent ?: "TranxCommunity/1.0")
                
                val existingLength = if (file.exists()) file.length() else 0
                if (existingLength > 0) {
                    connection.setRequestProperty("Range", "bytes=$existingLength-")
                }
                
                connection.connect()
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    
                    val inputStream = connection.inputStream
                    val raf = RandomAccessFile(file, "rw")
                    
                    if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                        raf.seek(existingLength)
                        task.downloadedLength = existingLength
                    } else {
                        // Server returned 200 OK, reset to start
                        raf.setLength(0)
                        task.downloadedLength = 0
                    }
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int = -1
                    
                    while (isActive && inputStream.read(buffer).also { bytesRead = it } != -1) {
                        raf.write(buffer, 0, bytesRead)
                        task.downloadedLength += bytesRead
                        // Throttling updates: save every 500KB
                        if (task.downloadedLength % (1024 * 500) == 0L) saveTasks() 
                    }
                    
                    raf.close()
                    inputStream.close()
                    
                    if (isActive) {
                        task.status = DownloadStatus.COMPLETED
                    } else {
                        task.status = DownloadStatus.PAUSED
                    }
                } else {
                    task.status = DownloadStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed", e)
                task.status = DownloadStatus.ERROR
            } finally {
                activeJobs.remove(task.id)
                saveTasks()
            }
        }
        activeJobs[task.id] = job
    }

    fun pauseDownload(task: DownloadTask) {
        activeJobs[task.id]?.cancel()
        task.status = DownloadStatus.PAUSED
        saveTasks()
    }

    fun deleteTask(task: DownloadTask) {
        pauseDownload(task)
        _tasks.remove(task)
        val file = File(task.filePath ?: "")
        if (file.exists()) file.delete()
        saveTasks()
    }

    fun openFileDirectory(task: DownloadTask) {
        val file = File(task.filePath ?: return)
        if (!file.exists()) return
        
        // Android 7.0+ requires FileProvider
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "*/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
