package ai.nnstreamer.ml.inference.offloading.di

import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStore
import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStoreImpl
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


@Module
object DataStoreModule {
    private const val SHARED_PREFERENCES = "shared_preferences"

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

    @Provides
    @Singleton
    fun providePreferencesStorage(dataStore: DataStore<Preferences>): PreferencesDataStore =
        PreferencesDataStoreImpl(dataStore)
}
