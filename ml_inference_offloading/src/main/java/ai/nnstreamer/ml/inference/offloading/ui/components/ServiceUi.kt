package ai.nnstreamer.ml.inference.offloading.ui.components

import ai.nnstreamer.ml.inference.offloading.ui.OffloadingServiceUiState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// TODO: This is a temporary function to create test buttons
@Composable
fun ButtonList(
    onLoadModel: () -> Unit,
) {
    Column {
        Button(onClick = { onLoadModel() }) {
            Text("Load models")
        }
    }
}

@Composable
fun ServiceList(
    services: List<OffloadingServiceUiState>,
    onClickStart: (Int) -> Unit,
    onClickStop: (Int) -> Unit,
    onClickDestroy: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        services.forEach { service ->
            Text(text = "Service ID: ${service.id}, State: ${service.pipelineState}, Port: ${service.port}")
            Button(onClick = { onClickStart(service.id) }) {
                Text(text = "Start")
            }
            Button(onClick = { onClickStop(service.id) }) {
                Text(text = "Stop")
            }
            Button(onClick = { onClickDestroy(service.id) }) {
                Text(text = "Destroy")
            }
        }
    }
}
