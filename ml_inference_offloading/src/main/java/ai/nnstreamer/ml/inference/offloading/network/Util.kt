package ai.nnstreamer.ml.inference.offloading.network

import ai.nnstreamer.ml.inference.offloading.App
import android.app.Service
import android.net.ConnectivityManager
import android.util.Log
import java.net.Inet4Address
import java.net.ServerSocket
import kotlin.concurrent.thread

const val TAG = "NetworkUtil"

/**
 * Returns the IP address based on whether the app is running on an emulator or not.
 *
 * @param isRunningOnEmulator Indicates whether the app is running on an emulator.
 * @return The IP address as a string.
 */
fun getIpAddress(isRunningOnEmulator: Boolean): String {
    val connectivityManager =
        App.context().getSystemService(Service.CONNECTIVITY_SERVICE) as ConnectivityManager
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

/**
 * Finds an available port for the server socket.
 *
 * @return The available port number as an integer.
 */
fun findPort(): Int {
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
