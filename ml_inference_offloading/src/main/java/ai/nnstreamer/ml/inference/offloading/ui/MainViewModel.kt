package ai.nnstreamer.ml.inference.offloading.ui

import ai.nnstreamer.ml.inference.offloading.data.OffloadingServiceRepositoryImpl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nnsuite.nnstreamer.Pipeline
import javax.inject.Inject

/**
 * The data class representing the UI state of an offloading service.
 *
 * @property id The unique identifier of the offloading service.
 * @property pipelineState The current state of the pipeline associated with the offloading service.
 * @property port The port number assigned to the offloading service.
 */
data class OffloadingServiceUiState(
    val id: Int = 0,
    val pipelineState: Pipeline.State = Pipeline.State.NULL,
    val port: Int = 0,
)

/**
 * A MainViewModel class responsible for managing UI-related data for the main screen.
 *
 * @property offloadingServiceRepositoryImpl The class that implements the repository pattern for the offloading services.
 */
class MainViewModel @Inject constructor(private val offloadingServiceRepositoryImpl: OffloadingServiceRepositoryImpl) :
    ViewModel() {
    /**
     * Mutable state flow containing a list of [OffloadingServiceUiState] objects.
     */
    private var _services: MutableStateFlow<List<OffloadingServiceUiState>> = MutableStateFlow(
        emptyList()
    )

    /**
     * State flow exposing a list of [OffloadingServiceUiState] objects.
     */
    val services: StateFlow<List<OffloadingServiceUiState>> = _services.asStateFlow()


    init {
        fetchServices()
    }

    /**
     * Fetches the list of offloading services from the repository and updates the UI state.
     */
    private fun fetchServices() {
        viewModelScope.launch {
            offloadingServiceRepositoryImpl.getAllOffloadingService().collect { services ->
                val newList: MutableList<OffloadingServiceUiState> = mutableListOf()
                services.forEach {
                    val newState = OffloadingServiceUiState(it.serviceId, it.state, it.port)
                    newList.add(newState)
                }
                _services.emit(newList)
            }
        }
    }
}
