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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private var mService: MainService? = null

    @Inject
    lateinit var mViewModel: MainViewModel

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
                    onCreateModel = { lifecycleScope.launch { mService?.createModels() } },
                    onLoadModel = { lifecycleScope.launch { mService?.loadModels() } }
                )
                ServiceList(
                    mViewModel.services.collectAsState().value,
                    onClickStart = { id -> mService?.startService(id) },
                    onClickStop = { id -> mService?.stopService(id) },
                    onClickDestroy = { id -> mService?.destroyService(id) })
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
