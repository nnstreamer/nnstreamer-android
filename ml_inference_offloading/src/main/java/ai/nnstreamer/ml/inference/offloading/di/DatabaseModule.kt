package ai.nnstreamer.ml.inference.offloading.di

import ai.nnstreamer.ml.inference.offloading.data.AppDatabase
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


/**
 * Database module for dependency injection. Provides instances of [AppDatabase] and DAOs.
 */
@Module
class DatabaseModule {
    /**
     * Provide an instance of [AppDatabase].
     * This method is annotated with @Provides to indicate that it should be used to create instances of [AppDatabase].
     *
     * @param context the application context to use to create the database.
     * @return an instance of [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideAppDatabase(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Provide an instance of [ModelDAO].
     *
     * @param database the instance of [AppDatabase] to use to get the DAO.
     * @return an instance of [ModelDAO].
     */
    @Provides
    @Singleton
    fun provideModelDAO(database: AppDatabase) = database.modelDao()

    /**
     * Provide an instance of [OffloadingServiceDAO].
     *
     * @param database the instance of [AppDatabase] to use to get the DAO.
     * @return an instance of [OffloadingServiceDAO].
     */
    @Provides
    @Singleton
    fun provideOffloadingServiceDAO(database: AppDatabase) = database.offloadingServiceDao()
}
