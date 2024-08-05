package ai.nnstreamer.ml.inference.offloading.data

import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStore.PreferencesKey.PREF_INC_CNT
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PreferencesDataStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesDataStore {
    override suspend fun getIncrementalCounter(): Int {
        dataStore.edit {
            it[PREF_INC_CNT] = dataStore.data.map { preferences ->
                (preferences[PREF_INC_CNT]?.plus(1)) ?: 0
            }.first()
        }

        return dataStore.data.map { preferences ->
            preferences[PREF_INC_CNT] ?: 0
        }.first()
    }
}
