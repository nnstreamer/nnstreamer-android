package ai.nnstreamer.ml.inference.offloading.ui.components

import ai.nnstreamer.ml.inference.offloading.ui.OffloadingServiceUiState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Composable function to load models in the device.
 *
 * This is a temporary function to create a button for the test purpose.
 * It will be replaced by a proper UI component later.
 *
 * @param onLoadModel Callback to be invoked when the "Load Models" button is clicked.
 */
@Composable
fun ButtonList(
    onLoadModel: () -> Unit,
) {
    // TODO: Create a proper UI component for this functionality. This is just a placeholder for now.
    Column {
        Button(onClick = { onLoadModel() }) {
            Text("Load models")
        }
    }
}

/**
 * Composable function to display a list of offloading services along with buttons to control their states.
 *
 * @param services The list of [OffloadingServiceUiState]s to be displayed.
 * @param onClickStart Callback to be invoked when the "Start" button is clicked.
 * @param onClickStop Callback to be invoked when the "Stop" button is clicked.
 * @param onClickDestroy Callback to be invoked when the "Destroy" button is clicked.
 */
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
