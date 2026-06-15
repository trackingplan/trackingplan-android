// Copyright (c) 2026 Trackingplan
package com.trackingplan.client.sdk.interception.urlconnection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Regression tests for POST body capture in the URLConnection instrumentation (issue #14293).
 * Adjust reads the response before closing the output stream, so the payload must reach the
 * request builder when interception finishes, not only when the stream is closed.
 */
public class InstrUrlConnectionPayloadCaptureTest {

    private static final byte[] BODY =
            "event_token=abc123&app_token=xyz789".getBytes(StandardCharsets.UTF_8);

    @Test
    public void payloadIsPublishedWhenResponseIsReadBeforeClosingOutputStream() throws Exception {
        // Goal: the builder holds the full payload by the time getInputStream() finishes
        // interception, even though the output stream has not been closed yet.
        final var builder = new RecordingRequestBuilder();
        final var connection = new InstrURLConnectionBase(new FakeHttpURLConnection(), builder);

        final var out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(new String(BODY, StandardCharsets.UTF_8));
        connection.getResponseCode();
        connection.getInputStream();

        assertArrayEquals(BODY, builder.payload);
        assertEquals(BODY.length, builder.payloadNumBytes);
    }

    @Test
    public void payloadIsPublishedWhenOutputStreamIsClosedBeforeReadingResponse() throws Exception {
        // Goal: the write -> close -> read order keeps publishing the payload at close time.
        final var builder = new RecordingRequestBuilder();
        final var connection = new InstrURLConnectionBase(new FakeHttpURLConnection(), builder);

        final var out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(new String(BODY, StandardCharsets.UTF_8));
        out.close();
        connection.getResponseCode();
        connection.getInputStream();

        assertArrayEquals(BODY, builder.payload);
        assertEquals(BODY.length, builder.payloadNumBytes);
    }

    private static final class RecordingRequestBuilder extends InstrumentRequestBuilder {

        byte[] payload;
        long payloadNumBytes;

        RecordingRequestBuilder() {
            super(null, "urlconnection");
        }

        @Override
        public void setRequestPayload(byte[] payload) {
            this.payload = payload;
            super.setRequestPayload(payload);
        }

        @Override
        public void setRequestPayloadNumBytes(long numBytes) {
            this.payloadNumBytes = numBytes;
            super.setRequestPayloadNumBytes(numBytes);
        }
    }

    private static final class FakeHttpURLConnection extends HttpURLConnection {

        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        FakeHttpURLConnection() throws Exception {
            super(new URL("https://app.adjust.com/event"));
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public OutputStream getOutputStream() {
            return body;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int getResponseCode() {
            return 200;
        }
    }
}
