package ai.nnstreamer.ml.inference.offloading

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

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

    private val TAG = "MainService"
    private lateinit var serviceHandler : MainHandler
    private lateinit var serviceLooper : Looper
    private lateinit var handlerThread: HandlerThread

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
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        Toast.makeText(this, "The MainService has been gone", Toast.LENGTH_SHORT).show()
    }
}