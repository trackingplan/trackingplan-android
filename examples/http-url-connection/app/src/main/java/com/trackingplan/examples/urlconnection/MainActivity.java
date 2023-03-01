package com.trackingplan.examples.urlconnection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.trackingplan.examples.urlconnection.tasks.SendGaEvents;

public class MainActivity extends AppCompatActivity {

    final private SendGaEvents backgroundTask = new SendGaEvents(this.getClass().getName(), 10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(view -> {
            final var myIntent = new Intent(this, MainActivity2.class);
            startActivity(myIntent);
        });

        new Thread(backgroundTask).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        backgroundTask.stop();
    }
}
