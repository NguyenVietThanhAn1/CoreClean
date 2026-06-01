package com.coreclean.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBaselineProfilesApi::class)
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "com.coreclean.app",
            startupMode = StartupMode.COLD,
            profileBlock = {
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

                // Skip onboarding screen for faster cold-start profiling
                device.executeShellCommand(
                    "am start -a android.intent.action.MAIN " +
                    "-n com.coreclean.app.debug/com.coreclean.app.MainActivity " +
                    "--ez skip_onboarding true"
                )

                // Cold start: launch app
                startActivityAndWait()

                // Wait for home screen to render (CoreClean title)
                device.wait(Until.hasObject(By.text("CoreClean")), 5_000)

                // Tap "Media Scanner" card
                val mediaCard = device.findObject(By.text("Media Scanner"))
                mediaCard?.click()

                // Wait for media grid to appear
                device.wait(Until.hasObject(By.res("com.coreclean.app:id/media_grid")), 5_000)

                // Scroll one page in the grid
                device.swipe(540, 1400, 540, 700, 20)

                // Navigate back to Home
                device.pressBack()
                device.wait(Until.hasObject(By.text("CoreClean")), 3_000)
            }
        )
    }
}
