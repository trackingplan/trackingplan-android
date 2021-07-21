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
package com.trackingplan.client.sdk.interception;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class InstrHttpOutputStream extends OutputStream {

    private static final int RESERVED_POST_SIZE_BYTES = 1024;
    private static final int MAX_POST_SIZE_BYTES = 100 * 1024;
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
            if (teeStream.size() < MAX_POST_SIZE_BYTES) {
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
            if (teeStream.size() < MAX_POST_SIZE_BYTES) {
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
            if (teeStream.size() < MAX_POST_SIZE_BYTES) {
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
        int boundedLen = Math.min(MAX_POST_SIZE_BYTES - teeStream.size(), len);
        teeStream.write(b, off, boundedLen);
    }
}
