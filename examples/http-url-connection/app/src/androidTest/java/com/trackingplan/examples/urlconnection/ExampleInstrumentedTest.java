package com.trackingplan.examples.urlconnection;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule;
import com.trackingplan.client.junit.TrackingplanJUnitRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    // Clear all app's SharedPreferences
    @Rule
    public ClearPreferencesRule clearPreferencesRule = new ClearPreferencesRule();

    @Rule
    public TrackingplanJUnitRule trackingplanRule = new TrackingplanJUnitRule("YOUR_TP_ID", "testing");

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
