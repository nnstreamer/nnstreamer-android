package ai.nnstreamer.ml.inference.offloading.data

import androidx.datastore.preferences.core.intPreferencesKey

interface PreferencesDataStore {
    suspend fun getIncrementalCounter(): Int

    object PreferencesKey {
        val PREF_INC_CNT = intPreferencesKey("incrementalCounter")
    }
}
