package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.ui.MainViewModel
import ai.nnstreamer.ml.inference.offloading.ui.components.ButtonList
import ai.nnstreamer.ml.inference.offloading.ui.components.ServiceList
import ai.nnstreamer.ml.inference.offloading.ui.theme.NnstreamerandroidTheme
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier

import javax.inject.Inject

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private var mService: Messenger? = null

    @Inject
    lateinit var mViewModel: MainViewModel

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = Messenger(service)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dependency Injection
        (application as App).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContent {
            NnstreamerandroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { }
            }
            Column {
                ButtonList(
                    onLoadModel = {
                        mService?.send(
                            Message.obtain(
                                null,
                                MessageType.LOAD_MODELS.value
                            )
                        )
                    }
                )
                ServiceList(
                    mViewModel.services.collectAsState().value,
                    onClickStart = { id ->
                        mService?.send(
                            Message.obtain(
                                null,
                                MessageType.START_MODEL.value,
                                id,
                                0
                            )
                        )
                    },
                    onClickStop = { id ->
                        mService?.send(
                            Message.obtain(
                                null,
                                MessageType.STOP_MODEL.value,
                                id,
                                0
                            )
                        )
                    },
                    onClickDestroy = { id ->
                        mService?.send(
                            Message.obtain(
                                null,
                                MessageType.DESTROY_MODEL.value,
                                id,
                                0
                            )
                        )
                    })
            }
        }
        startForegroundService(Intent(this, MainService::class.java))
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MainService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }
}
