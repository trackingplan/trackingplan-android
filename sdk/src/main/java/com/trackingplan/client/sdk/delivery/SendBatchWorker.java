// MIT License
//
// Copyright (c) 2021 Trackingplan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.trackingplan.client.sdk.delivery;

/*
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.trackingplan.client.sdk.TrackingplanClient;
import com.trackingplan.client.sdk.exceptions.TrackingplanSendException;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
 */

/**
 * WorkManager implementation of a batch worker
 */
/*
final public class SendBatchWorker extends Worker {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    public static final String TP_ID_KEY = "tp_id";
    public static final String SAMPLING_RATE_KEY = "sampling_rate";
    public static final String BATCH_ID_KEY = "batch_id";
    public static final String PAYLOAD_FILE_NAME_KEY = "payload_file";
    public static final String DELIVERED_TIME_MS_KEY = "delivered_time_ms";

    private static final long BATCH_LIFE_TIME_MS = 1 * 60 * 1000; // TODO: Change to 10 minutes

    public SendBatchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String tpId = getInputData().getString(TP_ID_KEY);
        float samplingRate = getInputData().getFloat(SAMPLING_RATE_KEY, (float) -1.0);
        long deliveredTimeMs = getInputData().getLong(DELIVERED_TIME_MS_KEY, -1);
        String fileName = getInputData().getString(PAYLOAD_FILE_NAME_KEY);
        long batchId = getInputData().getLong(BATCH_ID_KEY, -1);

        if (tpId == null || samplingRate == -1 || fileName == null || batchId == -1 || deliveredTimeMs == -1) {
            logger.warn("SendBatchWorker: Missing input data");
            return Result.failure();
        }

        try {

            if (deliveredTimeMs + BATCH_LIFE_TIME_MS < System.currentTimeMillis()) {
                throw new Exception("Batch timed out");
            }

            byte[] batchPayload = readPayload(fileName);
            sendTracks(batchPayload);
            logger.info("Raw tracks of batch " + batchId + " sent to Trackingplan");

            return Result.success();

        } catch (Exception ex) {
            logger.warn("Send failed and batch " + batchId + " will be discarded: " + ex.getMessage());
            return Result.failure();
        } finally {
            getApplicationContext().deleteFile(fileName);
        }
    }

    private byte[] readPayload(String fileName) throws IOException {
        try (FileInputStream fis = getApplicationContext().openFileInput(fileName)) {
            return StreamUtils.readAll(new BufferedInputStream(fis)).toByteArray();
        }
    }

    private void sendTracks(byte[] payload) throws IOException {

        HttpURLConnection conn = makeNewTracksConnection();

        try {
            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload);
            }
            int code = conn.getResponseCode();
            if (code != 204) {
                throw new TrackingplanSendException("Backend responded with " + code);
            }
        } catch (SocketTimeoutException ex) {
            throw new TrackingplanSendException("Connection to tracks timed out", ex);
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection makeNewTracksConnection() throws IOException {
        URL tracksEndPoint = new URL(TrackingplanClient.TRACKS_END_POINT);
        HttpURLConnection conn = (HttpURLConnection) tracksEndPoint.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }
}
*/