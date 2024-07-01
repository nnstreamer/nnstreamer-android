package ai.nnstreamer.ml.inference.offloading.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Model::class], version = 1, exportSchema = false)
abstract class ModelDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao

    companion object {
        @Volatile
        private var Instance: ModelDatabase? = null

        fun getDatabase(context: Context): ModelDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ModelDatabase::class.java, "model_database")
                    .build().also { Instance = it }
            }
        }
    }
}
