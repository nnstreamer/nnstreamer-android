package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.di.AppComponent
import ai.nnstreamer.ml.inference.offloading.di.DaggerAppComponent
import android.app.Application
import android.content.Context

class App : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance: App

        fun context(): Context {
            return instance.applicationContext
        }
    }

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(context())
    }

}
