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
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.windows.h.openfile.R
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Timer
import java.util.TimerTask

class Screenshot : Service() {

    private var fileUri: Uri? = null
    private val binder = ServiceBinder()
    private var step = 0
    private var find: DocumentFile? = null
    private var lastFile: File? = null
    private var fileSize: Long = 0
    private lateinit var context: Context
    private lateinit var timer: Timer
    private lateinit var handler: Handler
    private lateinit var notificationManager: NotificationManager
    private var running = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "send_message_to_qq_channel"
        private const val CHANNEL_NAME = "发送消息到qq"
    }

    inner class ServiceBinder : Binder() {
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
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("发送消息到qq")
                .setContentText("正在检测需要发送内容")
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
                    //takeScreenshot()
                    sendPicture()
                    //openPicture()
                    //open()
                }
            }, 0, 1000)
            running = true
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        timer.cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun takeScreenshot() {
        try {
            val path = Environment.getExternalStorageDirectory().path
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("screencap -p \"$path/\\\$MuMu12Shared/temp3.png\"\n")
                os.writeBytes("exit\n")
            }
            process.waitFor()
            process.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                when (fileSize) {
                    0L -> {
                        fileSize = tempFind.length()
                    }

                    tempFind.length() -> {
                        val file1 = File(this.filesDir, "picture_dir")
                        file1.mkdir()
                        lastFile?.delete()
                        val file2 = File(this.filesDir, "picture_dir/" + tempFind.name)
                        lastFile = file2
                        try {
                            contentResolver.openInputStream(tempFind.uri)?.use { input ->
                                FileOutputStream(file2).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            startActivity(
                                Intent(Intent.ACTION_VIEW)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    .setDataAndType(
                                        FileProvider.getUriForFile(
                                            this,
                                            "com.windows.h.openfile.file.provider",
                                            file2
                                        ), "image/png"
                                    )
                            )
                            step = 1
                        } catch (_: Exception) {
                        }
                    }

                    else -> {
                        fileSize = tempFind.length()
                    }
                }
            }
        } else if (step == 13) {
            tap(160, 160)
            step++
        } else if (step == 14) {
            find?.also { tempFind ->
                tempFind.name?.also { findName ->
                    val get =
                        "(.+)_(\\d+)_(\\d+)(.*)".toRegex().find(findName)?.groupValues?.get(2)
                    get?.also { groupId ->
                        input(groupId)
                    }
                }
            }
            step++
        } else if (step == 17) {
            tap(240, 240)
            step++
        } else if (step == 25) {
            tap(400, 750)
            step++
        } else if (step == 35) {
            open()
            step++
        } else if (step >= 43) {
            find?.delete()
            find = null
            fileSize = 0
            step = -5
        } else {
            step++
        }
    }

    fun processData(data: String) {
        fileUri = Uri.parse(data)
    }

    private fun openPicture() {
        fileUri?.also { tempUri ->
            val documentFile = DocumentFile.fromTreeUri(this, tempUri)
            find = documentFile?.listFiles()?.find { file ->
                file.name?.endsWith(".png", true) ?: false
            }
            find?.also { tempFind ->
                val file1 = File(this.filesDir, "picture_dir")
                file1.mkdir()
                val file2 = File(this.filesDir, "picture_dir/" + tempFind.name)
                try {
                    contentResolver.openInputStream(tempFind.uri)?.use { input ->
                        FileOutputStream(file2).use { output ->
                            input.copyTo(output)
                        }
                    }
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setDataAndType(
                                FileProvider.getUriForFile(
                                    this,
                                    "com.windows.h.openfile.file.provider",
                                    file2
                                ), "image/png"
                            )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

fun getForegroundPackageName(): String? {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("dumpsys activity | grep mFocusedApp\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        val stringBuilder = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
        }
        // 解析包名
        return ".*\\s+(\\S+)/(\\S+).*".toRegex().find(stringBuilder.toString())?.groupValues?.get(1)
    } catch (e: Exception) {
        return e.toString()
    }
}

fun tap(x: Int, y: Int) {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("input tap $x $y\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        process.waitFor()
        process.destroy()
    } catch (_: Exception) {
    }
}

fun input(str: String) {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("input text '$str'\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        process.waitFor()
        process.destroy()
    } catch (_: Exception) {
    }
}

fun home() {
    try {
        val keyCode = KeyEvent.KEYCODE_HOME
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("input keyevent '$keyCode'\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        process.waitFor()
        process.destroy()
    } catch (_: Exception) {
    }
}

fun open() {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("am start -n com.windows.h.openfile/com.windows.h.openfile.MainActivity\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        process.waitFor()
        process.destroy()
    } catch (_: Exception) {
    }
}