package com.trackingplan.examples.urlconnection.tasks;

import androidx.annotation.NonNull;

import com.trackingplan.examples.urlconnection.Utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendGaEvents implements Runnable {

    final private String prefix;
    final private int numEvents;
    private boolean stopped = false;

    public SendGaEvents(@NonNull String prefix, int numEvents) {
        this.prefix = prefix;
        this.numEvents = numEvents;
    }

    @Override
    public void run() {
        try {
            sendGaRandomGetRequests(50, 150);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        stopped = true;
    }

    private void sendGaRandomGetRequests(long minTimeBetweenMs, long maxTimeBetweenMs) throws IOException, InterruptedException {

        for (int i = 0; i < numEvents; i++) {

            if (stopped) {
                break;
            }

            String eventName = prefix + "_Random_" + Utils.getRandomAlphaNumericString(10);
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
            } finally {
                conn.disconnect();
            }

            if (stopped) {
                break;
            }

            long waitTimeMs = Utils.getRandomNumber(minTimeBetweenMs, maxTimeBetweenMs);
            Thread.sleep(waitTimeMs);
        }
    }
}
