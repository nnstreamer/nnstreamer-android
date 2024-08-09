package ai.nnstreamer.ml.inference.offloading.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.fromJson(json: String): T =
    fromJson<T>(json, object : TypeToken<T>() {}.type)

class StringMapConverter {
    @TypeConverter
    fun fromStringToStringMap(map: Map<String, String>): String {
        return Gson().toJson(map)
    }

    @TypeConverter
    fun toStringToStringMap(str: String): Map<String, String> {
        return try {
            Gson().fromJson<Map<String, String>>(str)
        } catch (e: Exception) {
            mapOf()
        }
    }

    @TypeConverter
    fun fromStringToStringListMap(map: Map<String, List<String>>): String {
        return Gson().toJson(map)
    }

    @TypeConverter
    fun toStringToStringListMap(str: String): Map<String, List<String>> {
        return try {
            Gson().fromJson<Map<String, List<String>>>(str)
        } catch (e: Exception) {
            mapOf()
        }
    }
}

@Database(entities = [Model::class, OffloadingService::class], version = 1, exportSchema = false)
@TypeConverters(StringMapConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    abstract fun offloadingServiceDao(): OffloadingServiceDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .build().also { Instance = it }
            }
        }
    }
}
