package ai.nnstreamer.ml.inference.offloading.network

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdRegistrationListener : NsdManager.RegistrationListener {
    private val TAG = "NsdRegistrationListener"

    override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
        Log.i(TAG, "on service registered " + nsdServiceInfo.serviceName)
    }

    override fun onRegistrationFailed(
        serviceInfo: NsdServiceInfo,
        errorCode: Int,
    ) {
        Log.e(TAG, "registration failed")
    }

    override fun onServiceUnregistered(arg0: NsdServiceInfo) {
        Log.i(TAG, "on service unregistered ")
    }

    override fun onUnregistrationFailed(
        serviceInfo: NsdServiceInfo,
        errorCode: Int,
    ) {
        Log.e(TAG, "unregistration failed")
    }
}
