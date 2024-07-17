package ai.nnstreamer.ml.inference.offloading.di

import ai.nnstreamer.ml.inference.offloading.data.ModelDatabase
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideModelDatabase(context: Context): ModelDatabase {
        return ModelDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideModelDAO(database: ModelDatabase) = database.modelDao()
}
