package com.trackingplan.examples.urlconnection;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule;
import com.trackingplan.client.junit.TrackingplanJUnit;
import com.trackingplan.client.junit.TrackingplanRule;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

/**
 * Instrumented test, which will execute on Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Rule
    public ClearPreferencesRule clearPreferencesRule = new ClearPreferencesRule();

    @Rule
    public TrackingplanRule trackingplanRule =
            TrackingplanJUnit.init("YOUR_TP_ID", "YOUR_ENVIRONMENT")
                    .tags(new HashMap<>() {{
                        put("test_title", "My test");
                        put("test_session_name", "My session");
                    }})
                    .dryRun()
                    .waitTimeMs(0)
                    .newRule();

    @Test
    public void testMainActivity() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertEquals("com.trackingplan.examples.urlconnection", appContext.getPackageName());
        }
    }

    @Test
    public void testSecondaryActivity() {
        try (ActivityScenario<MainActivity2> ignored = ActivityScenario.launch(MainActivity2.class)) {
            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertEquals("com.trackingplan.examples.urlconnection", appContext.getPackageName());
        }
    }
}
