package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.data.Classification
import ai.nnstreamer.ml.inference.offloading.data.ImageAnalyzer
import ai.nnstreamer.ml.inference.offloading.domain.MobilenetClassifier
import ai.nnstreamer.ml.inference.offloading.ui.MainViewModel
import ai.nnstreamer.ml.inference.offloading.ui.components.ButtonList
import ai.nnstreamer.ml.inference.offloading.ui.components.ServiceList
import ai.nnstreamer.ml.inference.offloading.ui.theme.NnstreamerandroidTheme
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject


data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int? = null,
    val screen: Any,
)

/**
 * The Main Activity class is the application's main entry point.
 *
 * It hosts the user interface to control the [MainService] component and demonstrates the ML use case,
 * which delegates the ML Task to [MainService].
 *
 * @property TAG Log tag for this class. Used for logging purposes.
 * @property mService Messenger instance to communicate with the [MainService].
 * @property mViewModel ViewModel instance for this activity.
 * @property connection ServiceConnection instance to bind to the [MainService].
 */
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

    /**
     * A lifecycle callback method that overrides [ComponentActivity.onCreate].
     *
     * This callback is invoked when the Activity is being created.
     *
     * @param savedInstanceState The saved instance state of the activity.
     */
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dependency Injection
        (application as App).appComponent.inject(this)
        super.onCreate(savedInstanceState)

        val info: PackageInfo =
            packageManager.getPackageInfo(applicationContext.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions
        if (permissions != null) {
            if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this, permissions, 0)
            }
        }

        setContent {
            NnstreamerandroidTheme {
                val navController = rememberNavController()
                val items = listOf(
                    NavigationItem(
                        title = "MLAgent",
                        selectedIcon = Icons.Filled.Api,
                        unselectedIcon = Icons.Outlined.Api,
                        screen = ScreenMLAgent,
                    ),
                    NavigationItem(
                        title = "Vision Examples",
                        selectedIcon = Icons.Filled.Videocam,
                        unselectedIcon = Icons.Outlined.Videocam,
                        screen = ScreenVisionExample,
                    ),
                    NavigationItem(
                        title = "Settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings,
                        screen = ScreenSettings(msg = "Setting UI Screen will be here")
                    ),
                )


                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) {
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var selectedItemIndex by rememberSaveable {
                        mutableIntStateOf(0)
                    }

                    ModalNavigationDrawer(
                        drawerContent = {
                            ModalDrawerSheet {
                                Spacer(modifier = Modifier.height(16.dp))
                                items.forEachIndexed { index, item ->
                                    NavigationDrawerItem(
                                        label = {
                                            Text(text = item.title)
                                        },
                                        selected = index == selectedItemIndex,
                                        onClick = {
                                            selectedItemIndex = index
                                            scope.launch {
                                                drawerState.close()
                                            }
                                            navController.navigate(item.screen) {
                                                popUpTo(navController.graph.id) {
                                                    inclusive = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (index == selectedItemIndex) {
                                                    item.selectedIcon
                                                } else {
                                                    item.unselectedIcon
                                                },
                                                contentDescription = item.title
                                            )
                                        },
                                        badge = {
                                            item.badgeCount?.let {
                                                Text(text = item.badgeCount.toString())
                                            }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    ) // NavigationDrawerItem
                                }
                            } // ModalDrawerSheet
                        },
                        drawerState = drawerState,
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(text = "NNStreamer Android") },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Menu"
                                            )
                                        }
                                    }
                                )
                            },
                        ) { paddingValues ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues.calculateTopPadding())
                            ) {
                                Column(modifier = Modifier.height(48.dp)) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = ScreenMLAgent
                                    ) {
                                        composable<ScreenMLAgent> {
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
                                                val offloadingServiceUiStates by mViewModel.services.collectAsStateWithLifecycle()
                                                ServiceList(
                                                    offloadingServiceUiStates,
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

                                        composable<ScreenVisionExample> {
                                            var classifications by remember {
                                                mutableStateOf(emptyList<Classification>())
                                            }
                                            val analyzer = remember {
                                                ImageAnalyzer(
                                                    MobilenetClassifier(
                                                        mService,
                                                        onResults = {
                                                            classifications = it
                                                        }),
                                                )
                                            }
                                            val controller = remember {
                                                LifecycleCameraController(applicationContext).apply {
                                                    setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                                                    setImageAnalysisAnalyzer(
                                                        ContextCompat.getMainExecutor(
                                                            applicationContext
                                                        ),
                                                        analyzer
                                                    )
                                                }
                                            }
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                CameraPreview(
                                                    controller,
                                                    Modifier
                                                        .fillMaxSize()
                                                )
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .align(Alignment.BottomCenter),
                                                ) {
                                                    classifications.forEach { classification ->
                                                        Text(
                                                            text = "${classification.label} (${classification.confidence}%)",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(colorScheme.secondaryContainer)
                                                                .padding(16.dp),
                                                            textAlign = TextAlign.Center,
                                                            fontSize = 20.sp,
                                                            color = colorScheme.onSecondaryContainer,
                                                        )
                                                    }
                                                } // Column
                                            } // Box
                                        }
                                        composable<ScreenSettings> {
                                            val args = it.toRoute<ScreenSettings>()
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(text = args.msg)
                                            }
                                        }
                                    }
                                }
                            }
                        }  // Scaffold
                    } // ModalNavigationDrawer
                } // Surface
            }
        }
        startForegroundService(Intent(this, MainService::class.java))
    }

    /**
     * A lifecycle callback method that overrides [ComponentActivity.onStart].
     *
     * This callback is invoked when the Activity is being started. It binds the Activity to the service.
     */
    override fun onStart() {
        super.onStart()
        Intent(this, MainService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * A lifecycle callback method that overrides [ComponentActivity.onStop].
     *
     * This callback is invoked when the Activity is being stopped. It unbinds the Activity from the service.
     */
    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    @Composable
    fun CameraPreview(
        controller: LifecycleCameraController,
        modifier: Modifier = Modifier
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current

        AndroidView(
            factory = {
                PreviewView(it).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = modifier,
        )
    }
}

@Serializable
object ScreenMLAgent

@Serializable
object ScreenVisionExample

@Serializable
data class ScreenSettings(
    val msg: String
)
