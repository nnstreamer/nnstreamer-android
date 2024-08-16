package ai.nnstreamer.ml.inference.offloading.data

import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStore.PreferencesKey.PREF_INC_CNT
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [PreferencesDataStore] using [DataStore]<[Preferences]>.
 * This class is responsible for implementing the abstract methods defined in [PreferencesDataStore].
 *
 * @property dataStore the [DataStore]<[Preferences]> instance used to store preferences.
 */
class PreferencesDataStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesDataStore {
    /**
     * Retrieve the incremental counter from the data store and increments it by 1.
     * It then stores the incremented value back into the data store.
     *
     * @return the incremented counter value. If no previous value exists, it returns 0.
     */
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
