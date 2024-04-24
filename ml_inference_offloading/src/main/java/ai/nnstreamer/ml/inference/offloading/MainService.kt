package ai.nnstreamer.ml.inference.offloading

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread


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
    // todo: Use internal network only now
    private val HOST_ADDR = "192.168.50.191"
    private val binder = LocalBinder()
    private lateinit var serviceHandler : MainHandler
    private lateinit var serviceLooper : Looper
    private lateinit var handlerThread: HandlerThread
    private var initialized = false
    private var port = -1
    private lateinit var tensorQueryServer: Pipeline
    private lateinit var model: File

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
        copyFilesToExternalDir()

        val path = getExternalFilesDir(null)!!.absolutePath
        model = File("$path/mobilenet_v1_1.0_224_quant.tflite")
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
        Toast.makeText(this, "The MainService has been gone", Toast.LENGTH_SHORT).show()
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

    fun startServer() {
        if (!isPortAvailable(port)) {
            port = findPort()
        }

        val desc = "tensor_query_serversrc host=" + HOST_ADDR + " port=" + port.toString() + " ! " +
                "other/tensor,format=static,dimension=(string)3:224:224:1,type=uint8,framerate=0/1 ! " +
                "tensor_filter framework=tensorflow-lite model=" + model.getAbsolutePath() + " ! " +
                "other/tensor,format=static,dimension=(string)1001:1,type=uint8,framerate=0/1 ! tensor_query_serversink async=false"
        tensorQueryServer = Pipeline(desc, null)
        tensorQueryServer.start()
    }

    fun stopServer() {
        tensorQueryServer.close()
    }
}
