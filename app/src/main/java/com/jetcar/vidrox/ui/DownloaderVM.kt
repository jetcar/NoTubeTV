package com.jetcar.vidrox.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
private const val UPDATE_DIRECTORY = "updates"

class UpdateViewModel : ViewModel() {

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    fun downloadApk(
        context: Context,
        url: String,
        tagName: String,
        onDownloaded: (File) -> Unit,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apkFile = downloadFile(context, url, tagName)
                withContext(Dispatchers.Main) {
                    onDownloaded(apkFile)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed $url", Toast.LENGTH_SHORT).show()
                    onError()
                }
            }
        }
    }

    private suspend fun downloadFile(context: Context, url: String, tagName: String): File {
        val client = HttpClient(OkHttp)
        val updatesDirectory = File(context.filesDir, UPDATE_DIRECTORY).apply {
            mkdirs()
        }
        val file = File(updatesDirectory, "VidroX_$tagName.apk")

        try {
            _downloadProgress.value = 0
            val response: HttpResponse = client.get(url)
            val total = response.contentLength() ?: -1L
            var downloaded = 0L

            withContext(Dispatchers.IO) {
                response.bodyAsChannel().toInputStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (total > 0) {
                                _downloadProgress.value = ((downloaded * 100) / total).toInt()
                            }
                        }
                    }
                }
            }
            if (total <= 0) {
                _downloadProgress.value = 100
            }
            return file
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally {
            client.close()
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) {
            Toast.makeText(
                context,
                "Downloaded update file is no longer available. Please download the update again.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            Toast.makeText(
                context,
                "Allow installs from this app, then try update again.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            return try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, APK_MIME_TYPE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "No installer found for the downloaded update.", Toast.LENGTH_LONG).show()
                false
            }
        }
    }
}
