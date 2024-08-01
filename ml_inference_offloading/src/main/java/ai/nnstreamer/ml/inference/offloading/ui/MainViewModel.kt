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

data class OffloadingServiceUiState(
    val id: Int = 0,
    val pipelineState: Pipeline.State = Pipeline.State.NULL,
    val port: Int = 0,
)

class MainViewModel @Inject constructor(private val offloadingServiceRepositoryImpl: OffloadingServiceRepositoryImpl) :
    ViewModel() {
    private var _services: MutableStateFlow<List<OffloadingServiceUiState>> = MutableStateFlow(
        emptyList()
    )

    val services: StateFlow<List<OffloadingServiceUiState>> = _services.asStateFlow()

    init {
        fetchServices()
    }

    private fun fetchServices() {
        viewModelScope.launch {
            offloadingServiceRepositoryImpl.getAllOffloadingService().collect { services ->
                val newList: MutableList<OffloadingServiceUiState> = mutableListOf()
                services.forEach {
                    val newState = OffloadingServiceUiState(it.serviceId, it.state, it.port)
                    newList.add(newState)
                }
                _services.value = newList.toList()
            }
        }
    }
}
