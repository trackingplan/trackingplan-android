package com.trackingplan.examples.okhttp;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.trackingplan.client.sdk.Trackingplan;
import com.trackingplan.examples.okhttp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getName();

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Trackingplan.init("YOUR_TP_ID")
                .enableDebug()
                .dryRun()
                .start(this);

        final var binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view -> {
            new Thread(new TestOkHttpRequests()).start();
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private static class TestOkHttpRequests implements Runnable {

        private final static String ECHO_GET = "https://postman-echo.com/get";
        private final static String ECHO_POST = "https://postman-echo.com/post";

        @Override
        public void run() {
            try {
                // Log.i(TAG, sendGetRequest());
                Log.i(TAG, sendPostRequest());
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.toString(), e);
            }
        }

        private String sendGetRequest() throws IOException {

            Call.Factory factory = request -> new OkHttpClient().newCall(request);

            Request request = new Request.Builder()
                    .url(ECHO_GET + "?id=okhttp3")
                    .build();

            try (Response response = factory.newCall(request).execute()) {
                return response.body().string();
            }
        }

        private String sendPostRequest() throws IOException, JSONException {

            Call.Factory factory = request -> new OkHttpClient().newCall(request);

            RequestBody body = RequestBody.create(
                    MediaType.get("application/json; charset=utf-8"),
                    makeJsonPayload("okhttp3")
            );

            Request request = new Request.Builder()
                    .url(ECHO_POST)
                    .post(body)
                    .build();

            try (Response response = factory.newCall(request).execute()) {
                return response.body().string();
            }
        }

        private String makeJsonPayload(String id) throws JSONException {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", id);
            return jsonObj.toString();
        }
    }
}
