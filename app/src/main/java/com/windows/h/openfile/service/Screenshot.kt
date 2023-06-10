package com.windows.h.openfile.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class Screenshot : Service() {

    private var isRunning = false
    private var fileUri: Uri? = null
    private val binder = MyBinder()
    private var step = 0
    private var find: DocumentFile? = null
    private var fileSize: Long = 0

    inner class MyBinder : Binder() {
        fun getService(): Screenshot {
            return this@Screenshot
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 获取传递的 Uri 参数
        val uriString = intent?.getStringExtra("uri")
        uriString?.also {
            fileUri = Uri.parse(uriString)
        }
        isRunning = true
        Thread {
            while (isRunning) {
                // 执行处理代码
                doSomething()
                sendPicture()
                Thread.sleep(1000)
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun doSomething() {
        takeScreenshot()
    }

    private fun sendPicture() {
        if (step == 0) {
            val tempFind = find
            if (tempFind == null) {
                fileUri?.also { tempUri ->
                    val documentFile = DocumentFile.fromTreeUri(this, tempUri)
                    find = documentFile?.listFiles()?.find { file ->
                        file.name?.endsWith(".png", true) ?: false
                    }
                }
            } else {
                if (fileSize == 0L) {
                    fileSize = tempFind.length()
                } else if (fileSize == tempFind.length()) {
                    val file1 = File(this.filesDir, "picture_dir")
                    file1.mkdir()
                    val file2 = File(this.filesDir, "picture_dir/" + tempFind.name)
                    try {
                        val inputStream = contentResolver.openInputStream(tempFind.uri)
                        val outputStream = FileOutputStream(file2)
                        inputStream?.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    val intent1 = Intent(Intent.ACTION_VIEW)
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val uri: Uri = FileProvider.getUriForFile(
                        this,
                        "com.windows.h.openfile.file.provider",
                        file2
                    )
                    intent1.setDataAndType(uri, "image/png")
                    startActivity(intent1)
                    step = 1
                } else {
                    fileSize = tempFind.length()
                }
            }
        }
    }

    fun processData(data: String) {
        fileUri = Uri.parse(data)
    }
}

fun takeScreenshot() {
    try {
        val process = Runtime.getRuntime().exec("su")
        val os = DataOutputStream(process.outputStream)
        os.writeBytes("screencap -p \"/sdcard/\\\$MuMu12Shared/temp3.png\"\n")
        os.writeBytes("exit\n")
        os.flush()
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}