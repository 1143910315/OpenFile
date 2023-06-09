package com.windows.h.openfile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.windows.h.openfile.ui.theme.OpenFileTheme
import java.io.File


class MainActivity : ComponentActivity() {
    var fileUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.button)
        val button1 = findViewById<Button>(R.id.button2)
        val editText = findViewById<EditText>(R.id.editTextTextMultiLine)
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { treeUri ->
            treeUri?.also {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                fileUri = treeUri
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


                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    val uri: Uri = FileProvider.getUriForFile(
                        this,
                        "com.windows.h.openfile.file.provider",
                        File(url.path + "/" + it.name)
                    )
                    intent.setDataAndType(uri, "image/png")
                    startActivity(intent)
                }
            }
        }
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