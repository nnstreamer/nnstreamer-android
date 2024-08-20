package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.data.ModelRepositoryImpl
import ai.nnstreamer.ml.inference.offloading.data.OffloadingService
import ai.nnstreamer.ml.inference.offloading.data.OffloadingServiceRepositoryImpl
import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStoreImpl
import ai.nnstreamer.ml.inference.offloading.network.NsdRegistrationListener
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.nnsuite.nnstreamer.NNStreamer
import org.nnsuite.nnstreamer.Pipeline
import java.net.Inet4Address
import java.net.ServerSocket
import javax.inject.Inject
import kotlin.concurrent.thread

enum class MessageType(val value: Int) {
    LOAD_MODELS(0),
    START_MODEL(1),
    STOP_MODEL(2),
    DESTROY_MODEL(3)
}

data class OffloadingServiceStatus(
    val pipeline: Pipeline,
    val registrationListener: RegistrationListener,
)

class MainService : Service() {
    private inner class MainHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MessageType.LOAD_MODELS.value ->
                    loadModels()
                MessageType.START_MODEL.value ->
                    startService(msg.arg1)
                MessageType.STOP_MODEL.value ->
                    stopService(msg.arg1)
                MessageType.DESTROY_MODEL.value ->
                    destroyService(msg.arg1)
                else -> super.handleMessage(msg)
            }
        }
    }

    private inner class PipelineCallback(serviceId: Int, modelId: Int, port: Int) :
        Pipeline.StateChangeCallback {
        val id = serviceId

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = modelId.toString()
            serviceType = "_nsd_offloading._tcp"
            setPort(port)
        }

        override fun onStateChanged(state: Pipeline.State?) {
            Log.i(TAG, "Service " + id.toString() + " change to " + state.toString())
            serviceMap[id]?.let { service ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (state != null) {
                        offloadingServiceRepositoryImpl.changeStateOffloadingService(
                            id,
                            state
                        )
                    }

                    when (state) {
                        Pipeline.State.UNKNOWN -> {}
                        Pipeline.State.NULL -> {}
                        Pipeline.State.READY -> {}
                        Pipeline.State.PAUSED -> {
                            nsdManager.apply {
                                unregisterService(service.registrationListener)
                            }
                        }

                        Pipeline.State.PLAYING -> {
                            nsdManager.apply {
                                registerService(
                                    serviceInfo,
                                    NsdManager.PROTOCOL_DNS_SD,
                                    service.registrationListener
                                )

                            }
                        }

                        null -> {
                            Log.e(TAG, "Pipeline callback state is NULL")
                        }
                    }
                }
            }
        }
    }

    private val TAG = "MainService"

    private val isRunningOnEmulator: Boolean
        get() = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.PRODUCT == "sdk_gphone64_arm64"
                || Build.FINGERPRINT == "robolectric"
                || Build.MANUFACTURER.contains("Geny")
    private lateinit var serviceHandler: MainHandler
    private lateinit var serviceLooper: Looper
    private lateinit var handlerThread: HandlerThread

    @Inject
    lateinit var modelsRepository: ModelRepositoryImpl

    @Inject
    lateinit var offloadingServiceRepositoryImpl: OffloadingServiceRepositoryImpl

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStoreImpl

    private var initialized = false
    private var serviceMap = mutableMapOf<Int, OffloadingServiceStatus>()
    private lateinit var nsdManager: NsdManager
    private lateinit var mMessenger: Messenger

    private fun startForeground() {
        // Get NotificationManager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // todo: Maintain a separate (data) class if needed
        // Declare a new Notification channel
        val chId = "mainService_ch0"
        val chName = "ch0"
        val chImportance = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel: NotificationChannel =
            NotificationChannel(chId, chName, chImportance).apply {
                val chDesc = "This is a notification channel to run this service in the foreground."

                description = chDesc
            }

        // Create the notification channel declared above
        notificationManager.createNotificationChannel(notificationChannel)
        val notification = NotificationCompat.Builder(this, chId).apply {
            setSmallIcon(android.R.drawable.ic_notification_overlay)
            setWhen(System.currentTimeMillis())
            setContentTitle("Main Service Notification")
            setContentText("The foreground service has been started!")
        }.build()


        // Check the FOREGROUND_SERVICE_SPECIAL_USE permission
        val spUsePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE
        )
        if (spUsePermission == PackageManager.PERMISSION_DENIED) {
            stopSelf()
            return
        }

        ServiceCompat.startForeground(
            this,
            100,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    override fun onCreate() {
        // Dependency Injection to AppComponent
        App.instance.appComponent.inject(this)

        initNNStreamer()
        if (!this.initialized) {
            Log.e(TAG, "Failed to initialize nnstreamer")
            stopSelf()
        }

        handlerThread =
            HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
                start()
            }
        this.applicationContext

        serviceLooper = handlerThread.looper
        serviceHandler = MainHandler(serviceLooper)
        nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)
        mMessenger = Messenger(serviceHandler)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Starting the MainService", Toast.LENGTH_SHORT).show()

        serviceHandler.obtainMessage().also { msg ->
            msg.arg1 = startId
            serviceHandler.sendMessage(msg)
        }

        startForeground()

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return mMessenger.binder
    }

    override fun onDestroy() {
        serviceMap.forEach { service ->
            val pipeline = service.value.pipeline
            pipeline.stop()
            pipeline.close()
            CoroutineScope(Dispatchers.IO).launch {
                offloadingServiceRepositoryImpl.deleteOffloadingService(service.key)
            }
        }
        serviceMap.clear()
        Toast.makeText(this, "The MainService has been gone", Toast.LENGTH_SHORT).show()
    }

    private fun initNNStreamer() {
        if (this.initialized) {
            return
        }
        try {
            initialized = NNStreamer.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message!!)
        } finally {
            if (initialized) {
                Log.i(TAG, "Version: " + NNStreamer.getVersion())
            } else {
                Log.e(TAG, "Failed to initialize NNStreamer")
            }
        }
    }

    // TODO: Add an ApplicationContext Parameter
    private fun getIpAddress(): String {
        val connectivityManager =
            App.context().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        var inetAddress = if (isRunningOnEmulator) "10.0.2.2" else "localhost"
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return inetAddress

        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address ?: continue

            when {
                address !is Inet4Address -> continue
                address.isLoopbackAddress -> continue
                else -> {
                    inetAddress = address.hostAddress ?: continue

                    break
                }
            }
        }

        return inetAddress
    }

    private fun findPort(): Int {
        var port = -1
        val portFinder = thread() {
            try {
                val serverSocket = ServerSocket(0)
                Log.i(TAG, "listening on port: " + serverSocket.localPort)
                port = serverSocket.localPort
                serverSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
        portFinder.join()
        return port
    }

    private fun loadModels() {
        val hostAddress = getIpAddress()
        val models = modelsRepository.getAllModelsStream()

        CoroutineScope(Dispatchers.IO).launch {
            models.collect {
                it.forEach { model ->
                    val serviceId = runBlocking {
                        preferencesDataStore.getIncrementalCounter()
                    }
                    // TODO: The following code for generation of the NNStreamer pipeline string
                    val mFile = App.context().getExternalFilesDir("models")?.resolve(model.models)
                    val inTypes = model.inputInfo["type"]?.let {
                        "types=${model.inputInfo["type"]?.joinToString(",")}"
                    } ?: ""
                    val inDims = model.inputInfo["dimension"]?.let {
                        "dimensions=(string)${model.inputInfo["dimension"]?.joinToString(",")}"
                    } ?: ""
                    val outTypes = model.outputInfo["type"]?.let {
                        "types=${model.outputInfo["type"]?.joinToString(",")}"
                    } ?: ""
                    val outDims = model.outputInfo["dimension"]?.let {
                        "dimensions=(string)${model.outputInfo["dimension"]?.joinToString(",")}"
                    } ?: ""
                    val filter =
                        "other/tensors,num_tensors=${model.inputInfo["type"]?.size ?: 1},format=static,${inDims},${inTypes},framerate=0/1 ! " +
                                "tensor_filter framework=tensorflow-lite model=${mFile} ! " +
                                "other/tensors,num_tensors=${model.outputInfo["type"]?.size ?: 1},format=static,${outDims},${outTypes},framerate=0/1"
                    val port = findPort()
                    val desc =
                        "tensor_query_serversrc id=" + serviceId.toString() + " host=" + hostAddress + " port=" +
                                port.toString() + " ! " + filter + " ! tensor_query_serversink async=false id=" + serviceId.toString()

                    val stateCb = PipelineCallback(serviceId, model.uid, port)
                    val pipeline = Pipeline(desc, stateCb)
                    val registrationListener = NsdRegistrationListener()

                    val offloadingServiceStatus = OffloadingServiceStatus(
                        pipeline,
                        registrationListener
                    )
                    serviceMap[serviceId] = offloadingServiceStatus

                    val offloadingService = OffloadingService(
                        serviceId,
                        model.uid,
                        port,
                        pipeline.state,
                        0
                    )
                    offloadingServiceRepositoryImpl.insertOffloadingService(offloadingService)
                }
            }
        }
    }

    private fun startService(id: Int) {
        serviceMap[id]?.pipeline?.start()
    }

    private fun stopService(id: Int) {
        serviceMap[id]?.pipeline?.stop()
    }

    private fun destroyService(id: Int) {
        serviceMap[id]?.pipeline?.close()
        serviceMap.remove(id)
        CoroutineScope(Dispatchers.IO).launch {
            offloadingServiceRepositoryImpl.deleteOffloadingService(id)
        }
    }
}
