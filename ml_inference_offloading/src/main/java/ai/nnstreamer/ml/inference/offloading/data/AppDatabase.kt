package ai.nnstreamer.ml.inference.offloading.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * An extension function that converts a JSON string to an object of type T.
 *
 * @param json the JSON string to convert.
 * @return the object of type T converted from the JSON string.
 */
inline fun <reified T> Gson.fromJson(json: String): T =
    fromJson<T>(json, object : TypeToken<T>() {}.type)

/**
 * A class that provides type conversion for maps of strings to strings and maps of string lists to strings in Room database.
 * This is used to store and retrieve such maps as strings in the database.
 */
class StringMapConverter {
    /**
     * Convert a map (of strings to strings) to a JSON string.
     *
     * @param map the map of string to string to convert
     * @return the JSON string representation of the map
     */
    @TypeConverter
    fun fromStringToStringMap(map: Map<String, String>): String {
        return Gson().toJson(map)
    }

    /**
     * Convert a JSON string to a map of strings to strings.
     *
     * @param str the JSON string to convert.
     * @return the map of strings to strings represented by the JSON string, or an empty map if conversion fails.
     */
    @TypeConverter
    fun toStringToStringMap(str: String): Map<String, String> {
        return try {
            Gson().fromJson<Map<String, String>>(str)
        } catch (e: Exception) {
            mapOf()
        }
    }

    /**
     * Convert a map (of strings to string lists) to a JSON string.
     *
     * @param map the map of strings to string lists to convert.
     * @return the JSON string representation of the map.
     */
    @TypeConverter
    fun fromStringToStringListMap(map: Map<String, List<String>>): String {
        return Gson().toJson(map)
    }

    /**
     * Convert a JSON string to a map of strings to strings.
     *
     * @param str the JSON string to convert.
     * @return the map of strings to string lists represented by the JSON string, or an empty map if the conversion fails.
     */
    @TypeConverter
    fun toStringToStringListMap(str: String): Map<String, List<String>> {
        return try {
            Gson().fromJson<Map<String, List<String>>>(str)
        } catch (e: Exception) {
            mapOf()
        }
    }
}

/**
 * The Room database provider for the application.
 * It provides access to the models and offloading services stored in the local database.
 */
@Database(entities = [Model::class, OffloadingService::class], version = 1, exportSchema = false)
@TypeConverters(StringMapConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * The DAO for the model entity. Provides methods to interact with the model table.
     */
    abstract fun modelDao(): ModelDao

    /**
     * The DAO for the offloading service entity. Provides methods to interact with the offloading service table.
     */
    abstract fun offloadingServiceDao(): OffloadingServiceDao

    /**
     * Companion object to create and get an instance of the database. Ensures only one instance of the database is created.
     */
    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        /**
         * Return an instance of the database. Creates a new instance if it doesn't exist.
         *
         * @param context The context in which the database is being accessed.
         * Used by Room database builder to create the database.
         */
        fun getDatabase(context: Context): AppDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .build().also { Instance = it }
            }
        }
    }
}
