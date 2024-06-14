package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.ui.theme.NnstreamerandroidTheme
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

// todo: Define DTO with generality and extensibility
data class ModelInfo(
    val name: String,
    val filter: String
)

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private var mService: MainService? = null

    inner class ModelViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        fun bind(info: ModelInfo) {
            val name = itemView.findViewById<TextView>(R.id.name)
            val start = itemView.findViewById<Button>(R.id.start)
            val stop = itemView.findViewById<Button>(R.id.stop)
            val port = itemView.findViewById<TextView>(R.id.port)

            name.text = info.name
            start.setOnClickListener(View.OnClickListener {
                val serverPort = mService?.startServer(info.name, info.filter)
                port.text = "Listening on port: " + serverPort.toString();
            })
            stop.setOnClickListener(View.OnClickListener {
                mService?.stopServer(info.name)
            })
        }
    }

    inner class ModelAdapter(private val modelInfos: ArrayList<ModelInfo>) : RecyclerView.Adapter<ModelViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.models, parent, false)

            return ModelViewHolder(view)
        }

        override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
            holder.bind(modelInfos[position])
        }

        override fun getItemCount(): Int = modelInfos.size
    }

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

        // todo: Use database instead of just ArrayList
        val modelList = ArrayList<ModelInfo>()
        val path = getExternalFilesDir(null)!!.absolutePath

        val mobileNet = File("$path/mobilenet_v1_1.0_224_quant.tflite")
        val mobileNetFilter = "other/tensor,format=static,dimension=(string)3:224:224:1,type=uint8,framerate=0/1 ! " +
                "tensor_filter framework=tensorflow-lite model=" + mobileNet.absolutePath + " ! " +
                "other/tensor,format=static,dimension=(string)1001:1,type=uint8,framerate=0/1"
        val mobileNetInfo = ModelInfo("MobileNet", mobileNetFilter)
        modelList.add(mobileNetInfo)

        val yolov8 = File("$path/yolov8s_float32.tflite")
        val yolov8Filter = "other/tensors,num_tensors=1,format=static,dimensions=3:224:224:1,types=float32,framerate=0/1 ! " +
                "tensor_filter framework=tensorflow-lite model=" + yolov8.absolutePath + " ! " +
                "other/tensors,num_tensors=1,types=float32,format=static,dimensions=1029:84:1,framerate=0/1"

        val yolov8Info = ModelInfo("Yolov8", yolov8Filter)
        modelList.add(yolov8Info)

        val recyclerView = findViewById<RecyclerView>(R.id.model_list)
        recyclerView.adapter = ModelAdapter(modelList)
        recyclerView.layoutManager = LinearLayoutManager(this, )
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
