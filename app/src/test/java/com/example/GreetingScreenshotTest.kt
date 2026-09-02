package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.NokiaDeviceEntity
import com.example.scanner.RadarScanState
import com.example.ui.screens.RadarScreen
import com.example.ui.theme.WiFiRadarTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleDevice = NokiaDeviceEntity(
      bssid = "C4:EA:1D:A3:L6:D8",
      ssid = "NOKIA-L6D8",
      firstSeenEpoch = System.currentTimeMillis() - 600000L,
      lastSeenEpoch = System.currentTimeMillis(),
      encounterCount = 5,
      lastRssi = -55,
      frequency = 2437,
      capabilities = "[WPA2-PSK-CCMP]",
      isNokiaTarget = true,
      radarAngleDeg = 45f
    )

    composeTestRule.setContent {
      WiFiRadarTheme {
        RadarScreen(
          devices = listOf(sampleDevice),
          nokiaThreats = listOf(sampleDevice),
          scanState = RadarScanState(isScanning = true, totalThreatsCount = 1, totalEncountersCount = 5),
          onTriggerScan = {},
          onToggleScanLoop = {},
          onInjectSample = {},
          onSelectDevice = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
