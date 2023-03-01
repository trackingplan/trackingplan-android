package com.trackingplan.examples.urlconnection;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.adevinta.android.barista.interaction.BaristaSleepInteractions;
import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule;
import com.trackingplan.client.junit.TrackingplanJUnit;

import org.junit.After;
import org.junit.Before;
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
public class NoRuleExampleInstrumentedTest {

    @Rule
    public ClearPreferencesRule clearPreferencesRule = new ClearPreferencesRule();

    @Before
    public void initTrackingplan() {
        System.out.println("BeforeTest");
        TrackingplanJUnit.init("YOUR_TP_ID", "YOUR_ENVIRONMENT")
                .tags(new HashMap<>() {{
                    put("test_title", "My test");
                    put("test_session_name", "My session");
                }})
                .dryRun()
                .start();
    }

    @After
    public void sendData() throws InterruptedException {
        System.out.println("AfterTest");
        TrackingplanJUnit.doSendAndStop(1500);
    }

    @Test
    public void testMainActivity() {
        System.out.println("test1");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertEquals("com.trackingplan.examples.urlconnection", appContext.getPackageName());
            BaristaSleepInteractions.sleep(1500);
        }
    }

    @Test
    public void testSecondaryActivity() {
        System.out.println("test2");
        try (ActivityScenario<MainActivity2> ignored = ActivityScenario.launch(MainActivity2.class)) {
            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertEquals("com.trackingplan.examples.urlconnection", appContext.getPackageName());
            BaristaSleepInteractions.sleep(1500);
        }
    }
}
