package com.trackingplan.examples.urlconnection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

public class Utils {
    public static OutputStream getOutputStream(HttpURLConnection conn, boolean compressed) throws IOException {
        if (compressed) {
            return new GZIPOutputStream(conn.getOutputStream());
        } else {
            return conn.getOutputStream();
        }
    }

    public static void setupJsonConnection(HttpURLConnection conn) {
        conn.setRequestProperty("Content-Type", "application/json");
    }

    public static long getRandomNumber(long min, long max) {
        return (long) (Math.random() * (1 + max - min) + min);
    }

    public static String getRandomAlphaNumericString(int n) {

        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }

        return sb.toString();
    }
}
