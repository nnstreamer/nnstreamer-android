package ai.nnstreamer.ml.inference.offloading.di

import ai.nnstreamer.ml.inference.offloading.MainActivity
import ai.nnstreamer.ml.inference.offloading.MainService
import ai.nnstreamer.ml.inference.offloading.providers.ModelFileProvider
import ai.nnstreamer.ml.inference.offloading.ui.MainViewModel
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [DatabaseModule::class, DataStoreModule::class])
interface AppComponent {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: MainActivity)
    fun inject(fileProvider: ModelFileProvider)
    fun inject(service: MainService)
    fun inject(viewModel: MainViewModel)
}
