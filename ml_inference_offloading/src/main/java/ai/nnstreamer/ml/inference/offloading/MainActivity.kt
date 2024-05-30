package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.ui.theme.NnstreamerandroidTheme
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
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

        startForegroundService(Intent(this, MainService::class.java))

        val start = findViewById<Button>(R.id.start)
        start.setOnClickListener(View.OnClickListener {
            val serverPort = mService?.startServer()
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
