package ai.nnstreamer.ml.inference.offloading.di

import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStore
import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStoreImpl
import ai.nnstreamer.ml.inference.offloading.di.DataStoreModule.SHARED_PREFERENCES
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton


/**
 * A Module to provide DataStore instances.
 * This module is mainly responsible for creating and configuring the [DataStore]<[Preferences]> instance.
 *
 * @property SHARED_PREFERENCES the name of the shared preferences file to store the data store preferences.
 */
@Module
object DataStoreModule {
    private const val SHARED_PREFERENCES = "shared_preferences"

    /**
     * Provide a [DataStore]<[Preferences]> instance.
     *
     * @param appContext the application context.
     * @return a [DataStore]<[Preferences]> instance.
     */
    @Provides
    @Singleton
    fun provideDataStore(appContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(SharedPreferencesMigration(appContext, SHARED_PREFERENCES)),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { appContext.preferencesDataStoreFile(SHARED_PREFERENCES) }
        )
    }

    /**
     * Provide a [PreferencesDataStore] instance.
     *
     * @param dataStore the instance of [DataStore]<[Preferences]>.
     * @return a [PreferencesDataStore] instance.
     */
    @Provides
    @Singleton
    fun providePreferencesStorage(dataStore: DataStore<Preferences>): PreferencesDataStore =
        PreferencesDataStoreImpl(dataStore)
}
