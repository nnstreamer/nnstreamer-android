package ai.nnstreamer.ml.inference.offloading.di

import ai.nnstreamer.ml.inference.offloading.data.AppDatabase
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideModelDAO(database: AppDatabase) = database.modelDao()

    @Provides
    @Singleton
    fun provideOffloadingServiceDAO(database: AppDatabase) = database.offloadingServiceDao()
}
