package ai.nnstreamer.ml.inference.offloading

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import org.nnsuite.nnstreamer.NNStreamer
import org.nnsuite.nnstreamer.Pipeline
import java.net.Inet4Address
import java.net.ServerSocket
import kotlin.concurrent.thread

// todo: Define DTO with generality and extensibility
data class ServerInfo(
    val pipeline: Pipeline,
    val port: Int,
    var status: Pipeline.State
)

class MainService : Service() {
    private inner class MainHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            try {
                // Sleep for 10 secs
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            stopSelf(msg.arg1)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MainService = this@MainService
    }

    private val TAG = "MainService"
    private val binder = LocalBinder()
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
    private lateinit var serviceHandler : MainHandler
    private lateinit var serviceLooper : Looper
    private lateinit var handlerThread: HandlerThread
    private var initialized = false
    private var serverInfoMap = mutableMapOf<String,ServerInfo>()

    private fun startForeground() {
        // Get NotificationManager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // todo: Maintain a separate (data) class if needed
        // Declare a new Notification channel
        val chId = "mainService_ch0"
        val chName = "ch0"
        val chImportance = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel : NotificationChannel = NotificationChannel(chId, chName, chImportance).apply {
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
        val spUsePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
        if (spUsePermission == PackageManager.PERMISSION_DENIED) {
            stopSelf()
            return
        }

        ServiceCompat.startForeground(this, 100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
   }

    override fun onCreate() {
        initNNStreamer()
        if (!this.initialized) {
            Log.e(TAG, "Failed to initialize nnstreamer")
            stopSelf()
        }

        handlerThread = HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
        }

        serviceLooper = handlerThread.looper
        serviceHandler = MainHandler(serviceLooper)
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
        return binder
    }

    override fun onDestroy() {
        serverInfoMap.values.forEach { status ->
            status.pipeline.close()
        }
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

    private fun isPortAvailable(port: Int): Boolean {
        var result = false
        if (port < 0) {
            return result
        }

        val portChecker = thread() {
            try {
                val serverSocket = ServerSocket(port)
                serverSocket.close()
                result = true
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
        portChecker.join()
        return result
    }

    // TODO: Add an ApplicationContext Parameter
    private fun getIpAddress(): String {
        val connectivityManager = App.context().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        var inetAddress = if (isRunningOnEmulator) "10.0.2.2" else "localhost"
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return inetAddress

        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address ?: continue

            when {
                address !is Inet4Address -> continue
                address.isLoopbackAddress -> continue
                else -> {
                    inetAddress = address.hostAddress?:continue

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

    fun getPort(name: String): Int {
        return serverInfoMap[name]?.port ?: -1
    }

    fun startServer(name:String, filter: String) {
        if (!serverInfoMap.containsKey(name)) {
            val hostAddress = getIpAddress()
            val port = findPort()
            val desc = "tensor_query_serversrc host=" + hostAddress + " port=" + port.toString() +
                    " ! " + filter + " ! tensor_query_serversink async=false"
            val tensorQueryServer = Pipeline(desc, null)
            serverInfoMap[name] = ServerInfo(tensorQueryServer, port, Pipeline.State.UNKNOWN)
        }

        serverInfoMap[name]?.let { modelStatus ->
            modelStatus.pipeline.start()
            modelStatus.status = Pipeline.State.PLAYING
        }
    }

    fun stopServer(name:String) {
        serverInfoMap[name]?.let { modelStatus ->
            modelStatus.pipeline.stop()
            modelStatus.status = Pipeline.State.PAUSED
        }
    }

    fun closeServer(name:String) {
        serverInfoMap[name]?.let { modelStatus ->
            modelStatus.pipeline.close()
            serverInfoMap.remove(name)
        }
    }
}
