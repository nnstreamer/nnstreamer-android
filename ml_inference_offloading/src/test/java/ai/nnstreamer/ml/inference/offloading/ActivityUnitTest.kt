package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.ui.OffloadingServiceUiState
import ai.nnstreamer.ml.inference.offloading.ui.components.ServiceList
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nnsuite.nnstreamer.Pipeline
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityUnitTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testServiceList() {
        val offloadingServiceUiStateList: List<OffloadingServiceUiState> = listOf(
            OffloadingServiceUiState(1, Pipeline.State.NULL, 3000)
        )

        composeRule.setContent {
            ServiceList(
                services = offloadingServiceUiStateList,
                onClickStart = { },
                onClickStop = { },
                onClickDestroy = { })
        }

        // Verify that the UI elements are displayed
        composeRule.onNodeWithText("Service ID: 1, State: NULL, Port: 3000").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("Stop").assertIsDisplayed()
        composeRule.onNodeWithText("Destroy").assertIsDisplayed()

        // Simulate button clicks
        composeRule.onNodeWithText("Start").performClick()
        composeRule.onNodeWithText("Stop").performClick()
        composeRule.onNodeWithText("Destroy").performClick()
    }
}
