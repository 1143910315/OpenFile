package com.windows.h.openfile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


class MainActivity : ComponentActivity() {
    private val prefUri = "uri"
    private var fileUri: Uri? = null
    private var myService: Screenshot? = null
    private var isServiceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as Screenshot.ServiceBinder
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
        val sharedPreferences = getSharedPreferences(prefUri, Context.MODE_PRIVATE)
        // 读取保存的 Uri
        val uriString = sharedPreferences.getString(prefUri, null)
        val intent = Intent(this, Screenshot::class.java)
        intent.putExtra("uri", uriString)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)
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
                editor.putString(prefUri, fileUri.toString())
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
                    val file1 = File(this.filesDir, "picture_dir")
                    file1.mkdir()
                    val file2 = File(this.filesDir, "picture_dir/" + it.name)
                    try {
                        contentResolver.openInputStream(it.uri)?.use { input ->
                            FileOutputStream(file2).use { output ->
                                input.copyTo(output)
                            }
                        }
                        startActivity(
                            Intent(Intent.ACTION_VIEW)
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
        button2.setOnClickListener {
            editText1.setText(dumpsysWindow())
        }
        button3.setOnClickListener {
            editText1.setText(dumpsysActivity())
            //editText1.setText(getForegroundPackageName() ?: "xx")
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

fun dumpsysWindow(): String {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("dumpsys window windows\n".toByteArray())
            output.write("exit\n".toByteArray())
        }
        val stringBuilder = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
        }
        process.waitFor()
        process.destroy()
        return stringBuilder.toString()
    } catch (e: Exception) {
        return e.toString()
    }
}

fun dumpsysActivity(): String {
    val process = Runtime.getRuntime().exec("su")
    process.outputStream.use { output ->
        output.write("dumpsys activity\n".toByteArray())
        output.write("exit\n".toByteArray())
    }
    val stringBuilder = StringBuilder()
    process.inputStream.use { input ->
        val reader = BufferedReader(InputStreamReader(input))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line).append("\n")
        }
    }
    process.waitFor()
    process.destroy()
    return stringBuilder.toString()
}

fun getForegroundPackageName(): String? {
    try {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { output ->
            output.write("dumpsys activity | grep mFocusedApp\n".toByteArray())
        }
        val stringBuilder = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
        }
        process.waitFor()
        process.destroy()
        // 解析包名
        return ".*\\s+(\\S+)/(\\S+).*".toRegex().find(stringBuilder.toString())?.groupValues?.get(1)
    } catch (e: Exception) {
        return e.toString()
    }
}