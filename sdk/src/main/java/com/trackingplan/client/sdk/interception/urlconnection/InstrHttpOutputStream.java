// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.interception.urlconnection;

import static com.trackingplan.client.sdk.TrackingplanConfig.MAX_REQUEST_BODY_SIZE_IN_BYTES;

import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class InstrHttpOutputStream extends OutputStream {

    private static final int RESERVED_POST_SIZE_BYTES = 1024;

    private final OutputStream outputStream;
    private final ByteArrayOutputStream teeStream;
    private final InstrumentRequestBuilder requestBuilder;
    private final InstrURLConnectionBase instrConn;

    private int bytesWritten = 0;

    public InstrHttpOutputStream(OutputStream outputStream, InstrURLConnectionBase instrConn, InstrumentRequestBuilder builder) {
        this.outputStream = outputStream;
        this.teeStream = new ByteArrayOutputStream(RESERVED_POST_SIZE_BYTES);
        requestBuilder = builder;
        this.instrConn = instrConn;
    }

    @Override
    public void write(int b) throws IOException {
        try {
            outputStream.write(b);
            if (teeStream.size() < MAX_REQUEST_BODY_SIZE_IN_BYTES) {
                teeStream.write(b);
            }
            bytesWritten++;
        } catch (IOException ex) {
            throw instrConn.finishInterceptionWithError(ex);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        try {
            outputStream.write(b);
            if (teeStream.size() < MAX_REQUEST_BODY_SIZE_IN_BYTES) {
                writePayload(b, 0, b.length);
            }
            bytesWritten += b.length;
        } catch (IOException ex) {
            throw instrConn.finishInterceptionWithError(ex);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            outputStream.write(b, off, len);
            if (teeStream.size() < MAX_REQUEST_BODY_SIZE_IN_BYTES) {
                writePayload(b, off, len);
            }
            bytesWritten += len;
        } catch (IOException ex) {
            throw instrConn.finishInterceptionWithError(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (bytesWritten > 0) {
                requestBuilder.setRequestPayload(teeStream.toByteArray());
                requestBuilder.setRequestPayloadNumBytes(bytesWritten);
            }
            outputStream.close();
        } catch (IOException ex) {
            throw instrConn.finishInterceptionWithError(ex);
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            outputStream.flush();
        } catch (IOException ex) {
            throw instrConn.finishInterceptionWithError(ex);
        }
    }

    private void writePayload(byte[] b, int off, int len) {
        int boundedLen = Math.min(MAX_REQUEST_BODY_SIZE_IN_BYTES - teeStream.size(), len);
        teeStream.write(b, off, boundedLen);
    }
}
