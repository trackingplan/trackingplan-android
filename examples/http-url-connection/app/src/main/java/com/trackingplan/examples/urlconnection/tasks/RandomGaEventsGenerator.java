package com.trackingplan.examples.urlconnection.tasks;

import android.util.Log;

import com.trackingplan.examples.urlconnection.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RandomGaEventsGenerator implements Runnable {

    private boolean finish = false;

    public void stop() {
        finish = true;
    }

    @Override
    public void run() {
        Log.i("AppExample", "Start");

        while (!finish) {

            Log.i("AppExample", "Run");

            try {
                long numRequests = Utils.getRandomNumber(4, 12);
                sendGaRandomGetRequests(numRequests, 0, 3000);
                sendGaRandomPostRequests(numRequests, 0, 3000);
            } catch (IOException | InterruptedException ex) {
                Log.v("AppExample", "Error: " + ex.getMessage());
            }

            try {
                long waitTimeMs = Utils.getRandomNumber(0, 20000);
                Thread.sleep(waitTimeMs);
            } catch (InterruptedException ex) {
                Log.v("AppExample", "Interrupted: " + ex.getMessage());
                finish = true;
            }
        }

        Log.i("AppExample", "Finish");
    }

    private void sendGaRandomGetRequests(long numRequests, long minTimeBetweenMs, long maxTimeBetweenMs) throws IOException, InterruptedException {

        for (int i = 0; i < numRequests; i++) {

            String eventName = "Random" + Utils.getRandomAlphaNumericString(10);
            String rawURL = "https://www.google-analytics.com/collect?v=1&_v=j81&a=1079976052" +
                    "&t=event&_s=4&dl=https%3A%2F%2Fdice.fm%2Fevent&dr=https%3A%2F%2Fwww.example.com%2F" +
                    "&ul=en&de=UTF-8&dt=Example&sd=24-bit&sr=2560x1080&vp=1691x709&je=0&ec=All" +
                    "&ea=" + eventName + "&_u=aGBAAEIJ~&jid=&gjid=&cid=1438716784.1587727392" +
                    "&uid=L4Zyw7Qf3mHk0OFDR%2FVFrkwMJ2w%3D" +
                    "&tid=UA-49561032-1&_gid=803700221.1587727392&z=836139612";

            URL url = new URL(rawURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                conn.getResponseCode();
                Log.i("AppExample", "Request sent");
            } finally {
                conn.disconnect();
            }

            long waitTimeMs = Utils.getRandomNumber(minTimeBetweenMs, maxTimeBetweenMs);
            Thread.sleep(waitTimeMs);
        }
    }

    private void sendGaRandomPostRequests(long numRequests, long minTimeBetweenMs, long maxTimeBetweenMs) throws IOException, InterruptedException {

        for (int i = 0; i < numRequests; i++) {

            boolean compressed = Utils.getRandomNumber(0, 1) > 0;

            String eventName = "Random" + Utils.getRandomAlphaNumericString(10);

            String postPayload = "v=1&_v=j81&a=1079976052&t=event&ni=1&_s=4&dl=https%3A%2F%2Funity.com%2F" +
                    "&dr=https%3A%2F%2Fwww.intercom.com%2Fcustomers%2Funity" +
                    "&dp=%2F&ul=en&de=UTF-8" +
                    "&dt=Unity%20Real-Time%20Development%20Platform%20%7C%203D%2C%202D%20VR%20%26%20AR%20Visualizations" +
                    "&sd=24-bit" +
                    "&sr=2560x1080" +
                    "&vp=1335x709&je=0" +
                    "&ec=All" +
                    "&ea=" + eventName +
                    "&el=100" +
                    "&_u=aGDACEALR~" +
                    "&jid=960160269" +
                    "&gjid=1015578035" +
                    "&cid=995732072.1587600203" +
                    "&tid=UA-2854981-61" +
                    "&_gid=1384444010.1587714237" +
                    "&_r=1" +
                    "&gtm=2wg4f05V25JL6" +
                    "&z=994029658";

            URL url = new URL("https://www.google-analytics.com/r/collect");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            if (compressed) {
                conn.setRequestProperty("Content-encoding", "gzip");
            }

            try (OutputStream out = Utils.getOutputStream(conn, compressed)) {
                byte[] buffer = postPayload.getBytes(StandardCharsets.UTF_8);
                out.write(buffer, 0, buffer.length);
            }

            try {
                conn.getResponseCode();
                Log.i("AppExample", "Request sent");
            } finally {
                conn.disconnect();
            }

            long waitTimeMs = Utils.getRandomNumber(minTimeBetweenMs, maxTimeBetweenMs);
            Thread.sleep(waitTimeMs);
        }
    }
}
