package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.di.AppComponent
import ai.nnstreamer.ml.inference.offloading.di.DaggerAppComponent
import android.app.Application
import android.content.Context

/**
 * Application class for providing Application context and DI component.
 *
 * @property instance The singleton instance of the application. This is used to provide the application context.
 * @property appComponent The Dagger component that provides dependencies for the application.
 */
class App : Application() {
    init {
        instance = this
    }

    /**
     * Companion object to provide the application context. This is used to access the application context from other classes.
     *
     * @property instance The singleton instance of the application. This is used to provide the application context.
     */
    companion object {
        lateinit var instance: App

        /**
         * Function to get the application context.
         * @return The application context.
         */
        fun context(): Context {
            return instance.applicationContext
        }
    }

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(context())
    }

}
