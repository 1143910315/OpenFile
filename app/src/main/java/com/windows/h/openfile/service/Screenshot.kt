package com.windows.h.openfile.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.windows.h.openfile.R
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Timer
import java.util.TimerTask

class Screenshot : Service() {

    private var fileUri: Uri? = null
    private val binder = ServiceBinder()
    private var find: DocumentFile? = null
    private var lastFile: File? = null
    private var fileSize: Long = 0
    private lateinit var context: Context
    private lateinit var timer: Timer
    private lateinit var handler: Handler
    private lateinit var notificationManager: NotificationManager
    private var running = false
    private val messageRegex = "([a-zA-Z]+)_(-?\\d+)_(-?\\d+)_(-?\\d+)\\.png".toRegex()
    private val packageRegex = ".*\\s+(\\S+)/(\\S+).*".toRegex()
    private var groupId = ""
    private var messageId = ""
    private var message = ""

    //1为mumu模拟器，2为夜神模拟器
    private val simulatorType = 2
    private val checkFileId = 110
    private var stepId = checkFileId

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "send_message_to_qq_channel"
        private const val CHANNEL_NAME = "发送消息到qq"
    }

    class ColorCheck(
        val checkX: Int,
        val checkY: Int,
        val checkR: Int,
        val checkG: Int,
        val checkB: Int
    )

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
                    //sendPicture()
                    //openPicture()
                    //open()
                    operate()
                }
            }, 1000)
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

    private fun operate() {
        try {
            when (stepId) {
                checkFileId -> {
                    stepId = checkFile()
                }

                90 -> {
                    stepId = idleOpenQQ()
                }

                in 91..99 -> {
                    stepId++
                }

                100 -> {
                    stepId = idleOpenSelf()
                }

                in 101..109 -> {
                    stepId++
                }

                89 -> {
                    stepId = checkFileSize()
                }

                88 -> {
                    stepId = openSelfBeforeOpenFile()
                }

                87 -> {
                    stepId = openFile()
                }

                86 -> {
                    stepId = checkInputGroupNumber()
                }

                85 -> {
                    stepId = selectFirst()
                }

                84 -> {
                    stepId = sendSharePicture()
                }

                83 -> {
                    stepId = inputWebsiteAddress()
                }

                82 -> {
                    stepId = checkOpenSuccess()
                }

                81 -> {
                    stepId = openSetting()
                }

                80 -> {
                    stepId = clearAppData()
                }

                79 -> {
                    stepId = openQQBeforeConsentAgreement()
                }

                78 -> {
                    stepId = consentAgreement()
                }

                77 -> {
                    stepId = clickLogin()
                }

                76 -> {
                    stepId = inputAccount()
                }

                75 -> {
                    stepId = inputPassword()
                }

                74 -> {
                    stepId = clickAgree()
                }

                73 -> {
                    stepId = loginQQ()
                }

                72 -> {
                    stepId = 88
                }

                in 66..71 -> {
                    stepId++
                }
            }
        } catch (e: Exception) {
            printfExceptionToFile(e)
        }
        timer.schedule(object : TimerTask() {
            override fun run() {
                // 执行处理代码
                //takeScreenshot()
                //sendPicture()
                //openPicture()
                //open()
                operate()
            }
        }, 1000)
    }

    private fun checkFile(): Int {
        fileUri?.also { tempUri ->
            val documentFile = DocumentFile.fromTreeUri(this, tempUri)
            find?.delete()
            find = documentFile?.listFiles()?.find { file ->
                val fileName = file.name
                if (fileName != null) {
                    if (fileName.endsWith(".png", true)) {
                        if (fileName.startsWith("msg", false)) {
                            messageRegex.find(fileName)?.groupValues?.also { gv ->
                                groupId = gv[2]
                                messageId = gv[3]
                                message = ""
                                return@find true
                            }
                        }
                        if (fileName.startsWith("gpt", false)) {
                            messageRegex.find(fileName)?.groupValues?.also { gv ->
                                groupId = gv[2]
                                messageId = gv[3]
                                message =
                                    "http://shiyu.server.miracleforest.cn:14527/ai?group=$groupId&message=$messageId"
                                return@find true
                            }
                        }
                    }
                }
                return@find false
            }
            if (find == null) {
                return 90
            }
            fileSize = 0
            return 89
        }
        return checkFileId
    }

    private fun idleOpenQQ(): Int {
        openApp("com.tencent.mobileqq/com.tencent.mobileqq.activity.SplashActivity")
        return 91
    }

    private fun idleOpenSelf(): Int {
        openApp("com.windows.h.openfile/com.windows.h.openfile.MainActivity")
        return 101
    }

    private fun checkFileSize(): Int {
        val tempFind = find
        if (tempFind != null) {
            fileSize = when (fileSize) {
                0L -> {
                    tempFind.length()
                }

                tempFind.length() -> {
                    return 88
                }

                else -> {
                    tempFind.length()
                }
            }
            return 89
        } else {
            return checkFileId
        }
    }

    private fun openSelfBeforeOpenFile(): Int {
        openApp("com.windows.h.openfile/com.windows.h.openfile.MainActivity")
        return 87
    }

    private fun openFile(): Int {
        val tempFind = find
        if (tempFind != null) {
            val file1 = File(this.filesDir, "picture_dir")
            file1.mkdir()
            lastFile?.delete()
            val file2 = File(this.filesDir, "picture_dir/" + tempFind.name)
            lastFile = file2
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
            return 82
        } else {
            return checkFileId
        }
    }

    private fun checkInputGroupNumber(): Int {
        val colorCheckList = if (simulatorType == 1) {
            arrayListOf(
                ColorCheck(40, 156, 176, 179, 191),
                ColorCheck(35, 151, 255, 255, 255)
            )
        } else {
            arrayListOf(
                ColorCheck(27, 104, 191, 191, 191),
                ColorCheck(23, 103, 255, 255, 255)
            )
        }
        if (checkColor(
                colorCheckList
            )
        ) {
            if (simulatorType == 1) {
                tap(160, 160)
            } else {
                tap(110, 110)
            }
            input(groupId)
            return 85
        } else {
            getForegroundPackageName()?.also { foregroundPackageName ->
                printfStringToFile("foreground", foregroundPackageName)
                if (foregroundPackageName == "com.windows.h.openfile") {
                    return 80
                }
            }
            checkBlocking()
        }
        return checkNoLogin()
    }

    private fun selectFirst(): Int {
        val colorCheckList = if (simulatorType == 1) {
            arrayListOf(
                ColorCheck(347, 268, 255, 255, 255),
                ColorCheck(300, 300, 245, 246, 250)
            )
        } else {
            arrayListOf(
                ColorCheck(170, 170, 255, 255, 255),
                ColorCheck(200, 200, 248, 249, 250)
            )
        }
        if (checkColor(
                colorCheckList
            )
        ) {
            if (simulatorType == 1) {
                tap(240, 240)
            } else {
                tap(160, 160)
            }
            return if (message == "") {
                84
            } else {
                83
            }
        }
        return 85
    }

    private fun sendSharePicture(): Int {
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 940, 255, 255, 255)
                )
            )
        ) {
            tap(400, 920)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 900, 255, 255, 255)
                )
            )
        ) {
            tap(400, 880)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 860, 255, 255, 255)
                )
            )
        ) {
            tap(400, 840)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 820, 255, 255, 255)
                )
            )
        ) {
            tap(400, 800)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 780, 255, 255, 255)
                )
            )
        ) {
            tap(400, 760)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 740, 255, 255, 255)
                )
            )
        ) {
            tap(400, 720)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 700, 255, 255, 255)
                )
            )
        ) {
            tap(400, 680)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 660, 255, 255, 255)
                )
            )
        ) {
            tap(400, 640)
            return 91
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(400, 620, 255, 255, 255)
                )
            )
        ) {
            tap(400, 600)
            return 91
        }
        return 84
    }

    private fun takeScreenshot() {
        val path = Environment.getExternalStorageDirectory().path
        val process = Runtime.getRuntime().exec("su")
        DataOutputStream(process.outputStream).use { os ->
            if (simulatorType == 1) {
                os.writeBytes("screencap -p \"$path/\\\$MuMu12Shared/sendToGroup/capture.png\"\n")
            } else if (simulatorType == 2) {
                os.writeBytes("screencap -p \"$path/Pictures/sendToGroup/capture.png\"\n")
            }
            os.writeBytes("exit\n")
        }
        process.waitFor()
        process.destroy()
    }

    fun processData(data: String) {
        fileUri = Uri.parse(data)
    }

    private fun printfExceptionToFile(e: Exception) {
        fileUri?.also { tempFileUrl ->
            val documentFile = DocumentFile.fromTreeUri(this, tempFileUrl)
            val createFile = documentFile?.createFile(
                "text/plain",
                "exception_record_" + System.currentTimeMillis() + ".txt"
            )
            // 转换异常堆栈信息为字符串
            val exceptionStackTrace = StringWriter().apply {
                e.printStackTrace(PrintWriter(this))
            }.toString()
            createFile?.also { tempCreateFile ->
                contentResolver.openOutputStream(tempCreateFile.uri)?.use { outputStream ->
                    BufferedOutputStream(outputStream).use { bufferedOutputStream ->
                        OutputStreamWriter(bufferedOutputStream).use { outputStreamWriter ->
                            outputStreamWriter.write(exceptionStackTrace)
                        }
                    }
                }
            }
        }
    }

    private fun printfStringToFile(fileName: String, plaintext: String) {
        fileUri?.also { tempFileUrl ->
            DocumentFile.fromTreeUri(this, tempFileUrl)?.also { documentFile ->
                documentFile.listFiles().forEach { temp ->
                    if (temp.name?.startsWith(fileName) == true && Math.random() < 0.1) {
                        temp.delete()
                    }
                }
                val createFile = documentFile.createFile(
                    "text/plain",
                    fileName + "_" + System.currentTimeMillis() + ".txt"
                )
                createFile?.also { tempCreateFile ->
                    contentResolver.openOutputStream(tempCreateFile.uri)?.use { outputStream ->
                        BufferedOutputStream(outputStream).use { bufferedOutputStream ->
                            OutputStreamWriter(bufferedOutputStream).use { outputStreamWriter ->
                                outputStreamWriter.write(plaintext)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkColor(colorCheckList: List<ColorCheck>): Boolean {
        takeScreenshot()
        fileUri?.also { tempUri ->
            val documentFile = DocumentFile.fromTreeUri(this, tempUri)
            val tempFind = documentFile?.listFiles()?.find { file ->
                file.name?.equals("capture.png") ?: false
            } ?: return false
            contentResolver.openInputStream(tempFind.uri)?.use { inputStream ->
                // 将PNG文件解码为Bitmap对象
                val bitmap = BitmapFactory.decodeStream(inputStream)
                return !colorCheckList.any { colorCheck ->
                    val checkX = colorCheck.checkX
                    val checkY = colorCheck.checkY
                    // 获取指定点的颜色值，例如(10, 20)
                    val color = bitmap.getPixel(checkX, checkY)
                    // 提取RGB分量
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    val currentTimeMillis = System.currentTimeMillis()
                    documentFile.listFiles().forEach { temp ->
                        if (temp.name?.startsWith("color") == true && Math.random() < 0.2) {
                            temp.delete()
                        }
                    }
                    documentFile.createFile(
                        "text/plain",
                        "color_($checkX,$checkY)_($r,$g,$b)_$currentTimeMillis.txt"
                    )
                    !(r == colorCheck.checkR && g == colorCheck.checkG && b == colorCheck.checkB)
                }
            }
        }
        return false
    }

    private fun checkBlocking() {
        if (checkColor(
                arrayListOf(
                    ColorCheck(113, 441, 27, 27, 31),
                    ColorCheck(108, 441, 242, 240, 244),
                    ColorCheck(103, 439, 82, 81, 85)
                )
            )
        ) {
            tap(230, 537)
        }
    }

    private fun inputWebsiteAddress(): Int {
        //320,940..530
        for (i in 940 downTo 530 step 30) {
            if (checkColor(
                    arrayListOf(
                        ColorCheck(320, i, 245, 245, 245),
                    )
                )
            ) {
                tap(320, i)
                input(message)
                return 84
            }
        }
        return 83
    }

    private fun getForegroundPackageName(): String? {
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
        return packageRegex.find(stringBuilder.toString())?.groupValues?.get(1)
    }

    private fun checkOpenSuccess(): Int {
        getForegroundPackageName()?.also { foregroundPackageName ->
            //printfStringToFile("foreground", foregroundPackageName)
            if (foregroundPackageName == "com.tencent.mobileqq") {
                return 86
            } else if (foregroundPackageName == "com.windows.h.openfile") {
                return 80
            }
        }
        return 82
    }

    private fun openSetting(): Int {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(Uri.fromParts("package", "com.tencent.mobileqq", null))
        )
        return 80
    }

    private fun clearAppData(): Int {
        val appPackName = "com.tencent.mobileqq"
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("pm clear $appPackName\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        process.waitFor()
        process.destroy()
        return 79
    }

    private fun openQQBeforeConsentAgreement(): Int {
        openApp("com.tencent.mobileqq/com.tencent.mobileqq.activity.SplashActivity")
        return 78
    }

    private fun consentAgreement(): Int {
        if (checkColor(
                arrayListOf(
                    ColorCheck(148, 455, 61, 129, 231),
                )
            )
        ) {
            tap(350, 600)
            return 77
        }
        return 78
    }

    private fun clickLogin(): Int {
        if (checkColor(
                arrayListOf(
                    ColorCheck(460, 860, 0, 153, 255),
                )
            )
        ) {
            tap(460, 860)
            return 76
        }
        return 77
    }

    private fun inputAccount(): Int {
        if (checkColor(
                arrayListOf(
                    ColorCheck(420, 230, 242, 243, 247),
                )
            )
        ) {
            tap(420, 230)
            input("1125076172")
            return 75
        }
        return 76
    }

    private fun inputPassword(): Int {
        if (checkColor(
                arrayListOf(
                    ColorCheck(420, 320, 242, 243, 247),
                )
            )
        ) {
            tap(420, 320)
            input("shiyu081015")
            return 74
        }
        return 75
    }

    private fun clickAgree(): Int {
        tap(160, 400)
        return 73
    }

    private fun loginQQ(): Int {
        tap(250, 550)
        return 66
    }

    private fun checkNoLogin(): Int {
        if (checkColor(
                arrayListOf(
                    ColorCheck(228, 111, 234, 28, 39),
                )
            )
        ) {
            return 76
        }
        if (checkColor(
                arrayListOf(
                    ColorCheck(148, 455, 61, 129, 231),
                )
            )
        ) {
            tap(350, 600)
            return 77
        }
        return 86
    }
}

fun tap(x: Int, y: Int) {
    val process = Runtime.getRuntime().exec("su")
    process.outputStream.use { output ->
        output.write("input tap $x $y\n".toByteArray())
        output.write("exit\n".toByteArray())
    }
    process.waitFor()
    process.destroy()
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

fun openApp(appPackName: String) {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("am start -n $appPackName\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        process.waitFor()
        process.destroy()
    } catch (_: Exception) {
    }
}