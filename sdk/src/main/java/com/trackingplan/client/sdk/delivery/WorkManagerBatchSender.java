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
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.trackingplan.client.sdk.BatchSender;
import com.trackingplan.client.sdk.TrackingplanConfig;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLogger;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
*/

/**
 * WorkManager implementation of a BatchSender
 *
 * NOTE: This implementation is disabled because of some issues with WorkManager.
 * For instance, it doesn't allow to pass payloads bigger than 10kB to workers.
 * Also, it presents some issues in multi-process scenario or when a library
 * and application use it.
 *
 * To overcome the first issue, this implementation pass the payload through files
 * that are first written to storage and then read by the worker.
 *
 * As a personal note, during some manual testing, WorkManager stopped executing tasks.
 * The reason was far from my understanding. So I preferred to disabled it.
 *
 * Moreover, our own TaskRunner is good enough for the purposes of this software.
 * The main difference is that our implementation will discard some tracks due to
 * application killed by the user, crashes or doze and stand-by modes. But this scenario
 * should be managed already by the Application using this library so the most likely is
 * that no requests are going to be send during doze/stand-by.
 */
/*
final public class WorkManagerBatchSender implements BatchSender {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private final TrackingplanConfig config;
    private final Context context;

    public WorkManagerBatchSender(@NonNull TrackingplanConfig config, @NonNull Context context) {
        this.config = config;
        this.context = context;
    }

    @Override
    public void send(@NonNull List<HttpRequest> batch, float samplingRate, long batchId) {

        String fileName = "tp-" + UUID.randomUUID().toString() + ".json";

        try {

            TrackBuilder builder = new TrackBuilder(config);

            byte[] payload = builder.createJsonPayload(batch, samplingRate)
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);

            Data inputData = new Data.Builder()
                    .putString(SendBatchWorker.TP_ID_KEY, config.getTpId())
                    .putLong(SendBatchWorker.BATCH_ID_KEY, batchId)
                    .putLong(SendBatchWorker.DELIVERED_TIME_MS_KEY, System.currentTimeMillis())
                    .putFloat(SendBatchWorker.SAMPLING_RATE_KEY, samplingRate)
                    .putString(SendBatchWorker.PAYLOAD_FILE_NAME_KEY, fileName)
                    .build();

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest sendBatchRequest =
                    new OneTimeWorkRequest.Builder(SendBatchWorker.class)
                            .setInputData(inputData)
                            .setConstraints(constraints)
                            .build();

            try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                fos.write(payload);
            }

            // Use APPEND policy to execute batches sequentially

            WorkManager.getInstance(context)
                    .beginUniqueWork(WorkManagerBatchSender.class.getName(), ExistingWorkPolicy.APPEND, sendBatchRequest)
                    .enqueue();

        } catch (Exception ex) {
            context.deleteFile(fileName);
            logger.warn("Send failed and batch will be discarded: " + ex.getMessage());
        }
    }
}
*/