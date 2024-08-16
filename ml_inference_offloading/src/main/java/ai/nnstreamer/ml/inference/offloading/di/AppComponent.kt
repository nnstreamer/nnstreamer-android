package ai.nnstreamer.ml.inference.offloading.di

import ai.nnstreamer.ml.inference.offloading.MainActivity
import ai.nnstreamer.ml.inference.offloading.MainService
import ai.nnstreamer.ml.inference.offloading.providers.ModelFileProvider
import ai.nnstreamer.ml.inference.offloading.ui.MainViewModel
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/**
 * Dagger component for dependency injection. This is the main entry point to the Dagger framework.
 */
@Singleton
@Component(modules = [DatabaseModule::class, DataStoreModule::class])
interface AppComponent {
    /**
     * Interface factory for creating instances of AppComponent.
     */
    @Component.Factory
    interface Factory {
        /**
         * Factory method to create an instance of AppComponent.
         *
         * @param context the application context.
         * @return an instance of AppComponent.
         */
        fun create(@BindsInstance context: Context): AppComponent
    }

    /**
     * A method to inject dependencies onto a specified activity.
     *
     * @param activity the activity to inject dependencies onto.
     */
    fun inject(activity: MainActivity)

    /**
     * A method to inject dependencies onto a specified file provider.
     *
     * @param fileProvider the file provider to inject dependencies onto.
     */
    fun inject(fileProvider: ModelFileProvider)

    /**
     * A method to inject dependencies onto a specified service.
     *
     * @param service the service to inject dependencies onto.
     */
    fun inject(service: MainService)

    /**
     * A method to inject dependencies onto a specified view model.
     *
     * @param viewModel the view model to inject dependencies onto.
     */
    fun inject(viewModel: MainViewModel)
}
