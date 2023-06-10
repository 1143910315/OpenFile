package com.windows.h.openfile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.windows.h.openfile.service.Screenshot
import com.windows.h.openfile.ui.theme.OpenFileTheme
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


class MainActivity : ComponentActivity() {
    private val PREF_URI = "uri"
    private var fileUri: Uri? = null
    private var myService: Screenshot? = null
    private var isServiceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as Screenshot.MyBinder
            myService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            myService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 获取 SharedPreferences 对象
        val sharedPreferences = getSharedPreferences(PREF_URI, Context.MODE_PRIVATE)
        // 读取保存的 Uri
        val uriString = sharedPreferences.getString(PREF_URI, null)
        uriString?.also {
            fileUri = Uri.parse(it)
            // 将 Uri 作为额外数据传递给 Service
            intent.putExtra("uri", fileUri.toString())
        }
        val intent = Intent(this, Screenshot::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)
        getRootPermission()
        takeScreenshot(this)
        val button = findViewById<Button>(R.id.button)
        val button1 = findViewById<Button>(R.id.button2)
        val button2 = findViewById<Button>(R.id.button3)
        val button3 = findViewById<Button>(R.id.button4)
        val editText = findViewById<EditText>(R.id.editTextTextMultiLine)
        val editText1 = findViewById<EditText>(R.id.editTextTextMultiLine2)
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { treeUri ->
            treeUri?.also {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                fileUri = treeUri
                sendDataToService(fileUri.toString())
                // 保存 Uri
                val editor = sharedPreferences.edit()
                editor.putString(PREF_URI, fileUri.toString())
                editor.apply()
                val documentFile = DocumentFile.fromTreeUri(this, treeUri)
                val stringBuilder = StringBuilder()
                documentFile?.listFiles()?.forEach { file ->
                    stringBuilder.append(file.name).append("\n")
                }
                editText.setText(stringBuilder.toString())
            }
        }
        button.setOnClickListener {
            // 权限未被授权，需要向用户请求权限
            requestPermissionLauncher.launch(null)
        }
        button1.setOnClickListener {
            fileUri?.also { url ->
                val documentFile = DocumentFile.fromTreeUri(this, url)
                val find = documentFile?.listFiles()?.find { file ->
                    file.name?.endsWith(".png", true) ?: false
                }
                find?.also {
                    //val file = File(url.path + "/" + it.name)
                    val file1 = File(this.filesDir, "picture_dir")
                    file1.mkdir()
                    val file2 = File(this.filesDir, "picture_dir/" + it.name)
                    //val resultUri = Uri.fromFile(file1)
                    //val aDocumentFile = DocumentFile.fromTreeUri(this, resultUri)
                    try {
                        //val inputStream = FileInputStream(file)
                        val inputStream = contentResolver.openInputStream(it.uri)
                        val outputStream = FileOutputStream(file2)
                        //val fileName = file.name
                        //val newFile = aDocumentFile?.createFile("image/png", fileName)
                        //val outputStream = newFile?.let { it1 ->
                        //    this.contentResolver.openOutputStream(
                        //        it1.uri)
                        //}
                        inputStream?.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    val intent1 = Intent(Intent.ACTION_VIEW)
                    //intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val uri: Uri = FileProvider.getUriForFile(
                        this,
                        "com.windows.h.openfile.file.provider",
                        file2
                    )
                    intent1.setDataAndType(uri, "image/png")
                    startActivity(intent1)
                }
            }
        }
        button2.setOnClickListener {
            editText1.setText(dumpsysWindow())
        }
        button3.setOnClickListener {
            editText1.setText(dumpsysActivity())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 解绑 Service
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun sendDataToService(data: String) {
        myService?.processData(data)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OpenFileTheme {
        Greeting("Android")
    }
}

fun getRootPermission(): Boolean {
    var process: Process? = null
    try {
        process = Runtime.getRuntime().exec("su")
        DataOutputStream(process.outputStream).use {
            it.writeBytes("echo \"test\" >/system/sd/temporary.txt\n")
            it.writeBytes("exit\n")
            it.flush()
        }
        process.waitFor()
    } catch (e: Exception) {
        return false
    } finally {
        try {
            process?.destroy()
        } catch (_: Exception) {
        }
    }
    return true
}

fun takeScreenshot(context: Context): Bitmap? {
    var bitmap: Bitmap? = null
    if (getRootPermission()) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("screencap -p /sdcard/temp.png\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            val file = File(context.getExternalFilesDir(null), "temp.png")
            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(file.absolutePath)
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return bitmap
}
fun dumpsysWindow(): String {
    val process = Runtime.getRuntime().exec("su")
    val outputStream = process.outputStream
    val inputStream = process.inputStream
    val errorStream = process.errorStream
    outputStream.write("dumpsys window windows\n".toByteArray())
    outputStream.write("exit\n".toByteArray())
    outputStream.flush()
    outputStream.close()
    val reader = BufferedReader(InputStreamReader(inputStream))
    var line: String?
    val stringBuilder = StringBuilder()
    while (reader.readLine().also { line = it } != null) {
        stringBuilder.append(line).append("\n")
    }
    return stringBuilder.toString()
}
fun dumpsysActivity(): String {
    val process = Runtime.getRuntime().exec("su")
    val outputStream = process.outputStream
    val inputStream = process.inputStream
    val errorStream = process.errorStream
    outputStream.write("dumpsys activity\n".toByteArray())
    outputStream.write("exit\n".toByteArray())
    outputStream.flush()
    outputStream.close()
    val reader = BufferedReader(InputStreamReader(inputStream))
    var line: String?
    val stringBuilder = StringBuilder()
    while (reader.readLine().also { line = it } != null) {
        stringBuilder.append(line).append("\n")
    }
    return stringBuilder.toString()
}