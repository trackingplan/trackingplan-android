package com.trackingplan.examples.urlconnection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.trackingplan.examples.urlconnection.tasks.SendGaEvents;
import com.trackingplan.examples.urlconnection.ui.BottomSheetFragment;

public class MainActivity extends AppCompatActivity {

    final private SendGaEvents backgroundTask = new SendGaEvents(10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button letStartButton = findViewById(R.id.button);
        letStartButton.setOnClickListener(view -> {
            final var myIntent = new Intent(this, MainActivity2.class);
            startActivity(myIntent);
        });

        final Button showBottomButton = findViewById(R.id.show_bottom_button);
        showBottomButton.setOnClickListener(view -> {
            BottomSheetFragment bottomSheet = new BottomSheetFragment();
            bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
        });

        new Thread(backgroundTask).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        backgroundTask.stop();
    }
}
