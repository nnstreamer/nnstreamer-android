package ai.nnstreamer.ml.inference.offloading.data

import androidx.datastore.preferences.core.intPreferencesKey

/**
 * Preferences Data Store interface. This is used to store and retrieve data from the preferences file.
 */
interface PreferencesDataStore {
    suspend fun getIncrementalCounter(): Int

    /**
     * Preference keys for storing data in the preferences file. These keys are used to identify the data stored in the preferences file.
     */
    object PreferencesKey {
        val PREF_INC_CNT = intPreferencesKey("incrementalCounter")
    }
}
