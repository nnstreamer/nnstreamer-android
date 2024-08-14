package ai.nnstreamer.ml.inference.offloading.network

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * A RegistrationListener for the services.
 *
 * Listener class for handling events related to network service registration using NSD (Network Service Discovery).
 */
class NsdRegistrationListener : NsdManager.RegistrationListener {
    private val TAG = "NsdRegistrationListener"

    /**
     * A lifecycle callback method that overrides [NsdManager.RegistrationListener.onServiceRegistered].
     *
     * @param nsdServiceInfo The information about the registered service.
     */
    override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
        Log.i(TAG, "on service registered " + nsdServiceInfo.serviceName)
    }

    /**
     * A lifecycle callback method that overrides [NsdManager.RegistrationListener.onRegistrationFailed].
     *
     * @param nsdServiceInfo The information about the service that failed to register.
     * @param errorCode The error code indicating the reason for failure.
     */
    override fun onRegistrationFailed(
        nsdServiceInfo: NsdServiceInfo,
        errorCode: Int,
    ) {
        Log.e(TAG, "registration failed")
    }

    /**
     * A lifecycle callback method that overrides [NsdManager.RegistrationListener.onServiceUnregistered].
     *
     * @param nsdServiceInfo The information about the unregistered service.
     */
    override fun onServiceUnregistered(nsdServiceInfo: NsdServiceInfo) {
        Log.i(TAG, "on service unregistered ")
    }

    /**
     * A lifecycle callback method that overrides [NsdManager.RegistrationListener.onUnregistrationFailed].
     *
     * @param nsdServiceInfo The information about the service that failed to unregister.
     * @param errorCode The error code indicating the reason for failure.
     */
    override fun onUnregistrationFailed(
        nsdServiceInfo: NsdServiceInfo,
        errorCode: Int,
    ) {
        Log.e(TAG, "unregistration failed")
    }
}
