package ai.nnstreamer.ml.inference.offloading.data

import ai.nnstreamer.ml.inference.offloading.App
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * The entity class for the models table.
 *
 * @constructor Creates a Model object.
 * @property framework The neural network framework supporting this model.
 * @property models The file names of this model.
 * @property inputInfo A map that represents the input information of the model.
 * @property outputInfo A map that represents the output information of the model.
 * @property optionalInfo A map that extensible information of the model.
 * @param uid The unique ID of the model.
 * @param name The name that represents this model.
 * @param jsonObject The JSON object corresponding to 'single'.
 * @param optionalJsonObject The JSON object corresponding to 'information'.
 */
@Entity(tableName = "models")
data class Model(
    @PrimaryKey
    val uid: Int = 0,
    val name: String,
) {
    constructor(
        uid: Int,
        name: String,
        jsonObject: JSONObject = JSONObject(),
        optionalJsonObject: JSONObject = JSONObject()
    ) : this(uid, name) {
        // TODO: The following is the parser for "single", which is a legacy conf
        runCatching {
            framework = jsonObject.getString("framework")
        }.exceptionOrNull()

        runCatching {
            val modelArray = jsonObject.getJSONArray("model")
            val modelPaths = mutableListOf<String>()

            for (i in 0 until modelArray.length()) {
                val model = modelArray.getString(i)

                modelPaths.add(File(model).name)
            }
            models = modelPaths.joinToString(",")
        }.onFailure { e ->
            when (e) {
                is JSONException -> {
                    val model = jsonObject.getString("model")
                    models = File(model).name
                }

                else -> {
                    throw e
                }
            }
        }.exceptionOrNull()

        listOf("input_info", "output_info").forEach { prop ->
            runCatching {
                val infoMap = mutableMapOf<String, MutableList<String>>(
                    "type" to mutableListOf(),
                    "dimension" to mutableListOf()
                )

                val info = jsonObject.getJSONArray(prop)

                for (i in 0 until info.length()) {
                    val obj = info.getJSONObject(i)

                    infoMap.keys.forEach { key ->
                        if (obj.has(key)) {
                            infoMap[key]?.add(obj.getString(key))
                        }
                    }
                }

                when (prop) {
                    "input_info" -> inputInfo = infoMap.toMap()
                    "output_info" -> outputInfo = infoMap.toMap()
                }
            }.exceptionOrNull()
        }

        val optionalInformationMap = mutableMapOf<String, String>()

        optionalJsonObject.keys().forEach { key ->
            optionalInformationMap[key] = ""
        }
        runCatching {
            optionalInformationMap.keys.forEach { key ->
                optionalInformationMap[key] = optionalJsonObject.getString(key)
            }
        }.exceptionOrNull()

        optionalInfo = optionalInformationMap
    }

    var framework: String = ""
    var models: String = ""

    @ColumnInfo(name = "input_info")
    var inputInfo: Map<String, List<String>> = mapOf()

    @ColumnInfo(name = "output_info")
    var outputInfo: Map<String, List<String>> = mapOf()

    var optionalInfo: Map<String, String> = mapOf()

    /**
     * Get NNS filter description string for the given model. This is used to create a new NNS pipeline instance.
     * @return NNS filter description string. It includes paths of the model files and other information.
     */
    fun getNNSFilterDesc(): String {
        val basePath = App.context().getExternalFilesDir("models")
        val modelPaths = models.split(",").run {
            joinToString(
                separator = ",",
                transform = { basePath?.resolve(it).toString() }
            )
        }
        val inTypes = inputInfo["type"]?.let {
            "types=${inputInfo["type"]?.joinToString(",")}"
        } ?: ""
        val inDims = inputInfo["dimension"]?.let {
            "dimensions=(string)${inputInfo["dimension"]?.joinToString(",")}"
        } ?: ""
        val outTypes = outputInfo["type"]?.let {
            "types=${outputInfo["type"]?.joinToString(",")}"
        } ?: ""
        val outDims = outputInfo["dimension"]?.let {
            "dimensions=(string)${outputInfo["dimension"]?.joinToString(",")}"
        } ?: ""
        val filter =
            "other/tensors,num_tensors=${inputInfo["type"]?.size ?: 1},format=static,${inDims},${inTypes},framerate=0/1 ! " +
                    "tensor_filter framework=tensorflow-lite model=${modelPaths} ! " +
                    "other/tensors,num_tensors=${outputInfo["type"]?.size ?: 1},format=static,${outDims},${outTypes},framerate=0/1"

        return filter
    }
}
