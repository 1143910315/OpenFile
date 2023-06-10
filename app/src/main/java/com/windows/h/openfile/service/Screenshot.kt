package com.windows.h.openfile.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.windows.h.openfile.R
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Timer
import java.util.TimerTask

class Screenshot : Service() {

    private var fileUri: Uri? = null
    private val binder = MyBinder()
    private var step = 0
    private var find: DocumentFile? = null
    private var fileSize: Long = 0
    private lateinit var context: Context
    private lateinit var timer: Timer
    private lateinit var handler: Handler
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "my_service_channel"
        private const val CHANNEL_NAME = "My Service Channel"
    }

    inner class MyBinder : Binder() {
        fun getService(): Screenshot {
            return this@Screenshot
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        timer = Timer()
        handler = Handler(Looper.getMainLooper())

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("My Service")
            .setContentText("Service is running...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        // 获取传递的 Uri 参数
        val uriString = intent?.getStringExtra("uri")
        uriString?.also {
            fileUri = Uri.parse(uriString)
        }

        timer.schedule(object : TimerTask() {
            override fun run() {
                // 执行处理代码
                doSomething()
                sendPicture()
            }
        }, 0, 1000)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
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
        } else if (step == 1) {
            handler.post {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("My Service")
                    .setContentText(getForegroundPackageName())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build()
                notificationManager.notify(NOTIFICATION_ID, notification)
                //Toast.makeText(context, "开始测试", Toast.LENGTH_LONG).show()
                //Toast.makeText(context, getForegroundPackageName(), Toast.LENGTH_LONG).show()
            }
            step += 20
        } else {
            step--
        }
    }

    fun processData(data: String) {
        fileUri = Uri.parse(data)
    }
}

fun takeScreenshot() {
    try {
        val path = Environment.getExternalStorageDirectory().path
        val process = Runtime.getRuntime().exec("su")
        val os = DataOutputStream(process.outputStream)
        os.writeBytes("screencap -p \"" + path + "/\\\$MuMu12Shared/temp3.png\"\n")
        os.writeBytes("exit\n")
        os.flush()
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getForegroundPackageName(): String? {
    var process: Process? = null
    var inputStream: InputStream? = null
    var result = ""

    try {
        process = Runtime.getRuntime().exec("su -c \"dumpsys window windows | grep mCurrentFocus\"")
        inputStream = process.inputStream
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        var line: String
        while (bufferedReader.readLine().also { line = it } != null) {
            result += line
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            inputStream?.close()
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // 解析包名
    val packageNameRegex = ".*\\s+(\\S+)/(\\S+)}.*".toRegex()
    val matchResult = packageNameRegex.find(result)
    return matchResult?.groupValues?.get(2)
}