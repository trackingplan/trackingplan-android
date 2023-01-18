package com.trackingplan.examples.urlconnection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.trackingplan.client.sdk.Trackingplan;
import com.trackingplan.examples.urlconnection.tasks.TestGaEvents;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Trackingplan.init("YOUR_TP_ID")
                // .environment("PRODUCTION")
                // .sourceAlias("Android Example")
                // .tags(YOUR_TAGS)
                // .customDomains(YOUR_CUSTOM_DOMAINS)
                .enableDebug()
                .dryRun()
                .start(this);

        // Trackingplan.stop();

        setContentView(R.layout.activity_main);
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(view -> {
            final var myIntent = new Intent(this, MainActivity2.class);
            startActivity(myIntent);
        });

        // new Thread(new RandomGaEventsGenerator()).start();
        new Thread(new TestGaEvents()).start();
        // new Thread(new TestUrlConnection()).start();
    }
}
