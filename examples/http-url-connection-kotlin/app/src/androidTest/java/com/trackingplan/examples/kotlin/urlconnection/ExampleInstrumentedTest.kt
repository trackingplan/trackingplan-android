package com.trackingplan.examples.kotlin.urlconnection

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trackingplan.client.junit.TrackingplanJUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val trackingplanRule = TrackingplanJUnit.init("YOUR_TP_ID", "YOUR_ENVIRONMENT")
        .tags(
            mapOf(
                "test_title" to "My Test",
                "test_session_name" to "My Session"
            )
        )
        .dryRun()
        .newRule()

//    @Before
//    fun initTrackingplan() {
//        TrackingplanJUnit.init("TP369979", "OnDemandBuild")
//            .tags(
//                mapOf(
//                    "test_title" to "My Test",
//                    "test_session_name" to "My Session"
//                )
//            )
//            .dryRun()
//            .start()
//    }
//
//    @After
//    @Throws(InterruptedException::class)
//    fun sendData() {
//        TrackingplanJUnit.doSendAndStop()
//    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.trackingplan.examples.kotlin.urlconnection", appContext.packageName)
    }
}
