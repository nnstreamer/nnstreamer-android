package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.ui.theme.NnstreamerandroidTheme
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private var mService: MainService? = null

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MainService.LocalBinder
            mService = binder.getService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mService = null
        }
    }
    private fun copyFilesToExternalDir() {
        val am = resources.assets
        var files: Array<String>? = null

        try {
            files = am.list("models/")
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "#### Failed to get asset file list")
            e.printStackTrace()
            return
        }

        // Copy files into app-specific directory.
        for (filename in files!!) {
            try {
                val inFile = am.open("models/$filename")
                val outDir = getExternalFilesDir(null)!!.absolutePath
                val outFile = File(outDir, filename)
                val out: OutputStream = FileOutputStream(outFile)

                val buffer = ByteArray(1024)
                var read: Int
                while ((inFile.read(buffer).also { read = it }) != -1) {
                    out.write(buffer, 0, read)
                }

                inFile.close()
                out.flush()
                out.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy file: $filename")
                e.printStackTrace()
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NnstreamerandroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { }
            }
        }
        setContentView(R.layout.activity_main)

        copyFilesToExternalDir()
        startForegroundService(Intent(this, MainService::class.java))

        val start = findViewById<Button>(R.id.start)
        start.setOnClickListener(View.OnClickListener {
            val path = getExternalFilesDir(null)!!.absolutePath
            val model = File("$path/mobilenet_v1_1.0_224_quant.tflite")
            val filter = "other/tensor,format=static,dimension=(string)3:224:224:1,type=uint8,framerate=0/1 ! " +
                    "tensor_filter framework=tensorflow-lite model=" + model.getAbsolutePath() + " ! " +
                    "other/tensor,format=static,dimension=(string)1001:1,type=uint8,framerate=0/1"
            val serverPort = mService?.startServer(filter)
            val portTextView = findViewById<TextView>(R.id.port)
            portTextView.text = "Listening on port: " + serverPort.toString();
        })

        val stop = findViewById<Button>(R.id.stop)
        stop.setOnClickListener(View.OnClickListener {
            mService?.stopServer()
        })
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MainService::class.java).also { intent->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }
}
