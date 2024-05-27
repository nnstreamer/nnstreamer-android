package ai.nnstreamer.ml.inference.offloading

import android.app.Application
import android.content.Context

class App : Application() {
    init{
        instance = this
    }

    companion object {
        lateinit var instance: App

        fun context() : Context {
            return instance.applicationContext
        }
    }
}
