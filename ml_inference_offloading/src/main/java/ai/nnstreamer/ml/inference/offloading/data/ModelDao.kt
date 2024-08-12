package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A Data Access Object (DAO) interface for the [Model] class. */
@Dao
interface ModelDao {
    /**
     * Insert a [Model] into the models table. If the model already exists, replace it.
     *
     * @param model the [Model] to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(model: Model)

    /**
     * Update the given [Model].
     *
     * @param model the [Model] to be updated.
     */
    @Update
    suspend fun update(model: Model)

    /**
     * Delete the given [Model] from the models table.
     *
     * @param model the [Model] to be deleted.
     */
    @Delete
    suspend fun delete(model: Model)

    /**
     * Retrieve a [Model] by [uid] from the models table.
     *
     * @param uid the unique identifier that represents a specific [Model] in the table
     * @return A [Flow] of the matching [Model]s.
     */
    @Query("SELECT * from models WHERE uid = :uid")
    fun getModel(uid: Int): Flow<Model>

    /**
     * Retrieve all [Model]s from the models table.
     *
     * @return A [Flow] of the list consisting of all [Model]s in the table.
     */
    @Query("SELECT * from models ORDER BY name ASC")
    fun getAllModels(): Flow<List<Model>>
}
