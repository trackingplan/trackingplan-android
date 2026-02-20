// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.BeforeTest

actual abstract class BaseTest {
    @BeforeTest
    fun initPlatform() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ContextProvider.init(context)
    }
}
