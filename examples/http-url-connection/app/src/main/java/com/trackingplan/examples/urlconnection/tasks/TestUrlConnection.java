package com.trackingplan.examples.urlconnection.tasks;

import com.trackingplan.examples.urlconnection.TestFailedException;
import com.trackingplan.examples.urlconnection.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class TestUrlConnection implements Runnable {

    private final static String ECHO_GET = "//postman-echo.com/get";
    private final static String ECHO_POST = "//postman-echo.com/post";

    @Override
    public void run() {
        try {
            runTests(false);
            runTests(true);
            testErrorStream();
            test404Error();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void runTests(boolean useHttps) throws IOException, JSONException {

        String protocol = useHttps ? "https" : "http";
        String echoGet = protocol + ":" + ECHO_GET;
        String echoPost = protocol + ":" + ECHO_POST;

        testURLOpenConnection(new URL(echoGet + "?id=URL.openConnection"), "URL.openConnection");
        testURLOpenConnectionProxy(new URL(echoGet + "?id=URL.openConnection(proxy)"), "URL.openConnection(proxy)");
        testURLOpenStream(new URL(echoGet + "?id=URL.openStream"), "URL.openStream");
        testURLGetContent(new URL(echoGet + "?id=URL.getContent"), "URL.getContent");
        testURLGetContentClass(new URL(echoGet + "?id=URL.getContent(classes)"), "URL.getContent(classes)");
        testURLConnectionGetContent(new URL(echoGet + "?id=URLConnection.getContent"), "URLConnection.getContent");
        testURLConnectionGetContentClass(new URL(echoGet + "?id=URLConnection.getContent(classes)"), "URLConnection.getContent(classes)");
        testURLConnectionGetResponseCode(new URL(echoGet + "?id=URLConnection.getResponseCode"), "URLConnection.getResponseCode");
        testPayloadInterception(new URL(echoPost), makeJsonPayload("post"), "post");
        testUserAgent(new URL(echoGet), "TPUserAgent");
    }

    private void testURLOpenConnection(URL url, String expected) throws IOException {

        System.out.print("Test URL.openConnection\t");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);

        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private void testURLOpenConnectionProxy(URL url, String expected) throws IOException {

        System.out.print("Test URL.openConnection(Proxy)\t");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        Utils.setupJsonConnection(conn);

        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private void testURLOpenStream(URL url, String expected) throws IOException {

        System.out.print("Test URL.openStream\t");

        try (InputStream in = url.openStream()) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void testURLGetContent(URL url, String expected) throws IOException {

        System.out.print("Test URL.getContent\t");

        try (InputStream in = new BufferedInputStream((InputStream) url.getContent())) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void testURLGetContentClass(URL url, String expected) throws IOException {

        System.out.print("Test URL.getContentClass\t");

        try (InputStream in = new BufferedInputStream((InputStream) url.getContent(new Class<?>[]{InputStream.class}))) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void testURLConnectionGetContent(URL url, String expected) throws IOException {

        System.out.print("Test URLConnection.getContent\t");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);

        try (InputStream in = new BufferedInputStream((InputStream) conn.getContent())) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void testURLConnectionGetContentClass(URL url, String expected) throws IOException {

        System.out.print("Test URLConnection.getContentClass\t");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);

        try (InputStream in = new BufferedInputStream((InputStream) conn.getContent(new Class<?>[]{InputStream.class}))) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void testURLConnectionGetResponseCode(URL url, String expected) throws IOException {

        System.out.print("Test URLConnection.getResponseCode\t");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);

        try {
            int code = conn.getResponseCode();
            if (code != 200) throw new TestFailedException("Test failed");
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private void testPayloadInterception(URL url, String jsonPayload, String expected) throws IOException {

        System.out.print("Test JSON Payload\t");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream out = conn.getOutputStream()) {
            byte[] buffer = jsonPayload.getBytes("utf-8");
            out.write(buffer, 0, buffer.length);
        }

        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            readAnswerAndCheckId(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private void testUserAgent(URL url, String expected) throws IOException {

        System.out.print("Test User-Agent\t");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);
        conn.setRequestProperty("User-Agent", expected);

        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            readAnswerAndCheckUserAgent(in, expected);
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private void testErrorStream() throws IOException {

        System.out.print("Test Error Stream\t");

        URL url = new URL("https://reqres.in/api/users/23");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);

        try {
            InputStream in = new BufferedInputStream(conn.getErrorStream());
            String answer = convertInputStreamToString(in);
            if (!answer.equals("{}")) {
                throw new TestFailedException("Test Failed");
            }
            System.out.println("Pass");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private void test404Error() throws IOException {

        System.out.print("Test 404 error\t");

        URL url = new URL("https://reqres.in/api/users/24");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Utils.setupJsonConnection(conn);

        try {
            if (conn.getResponseCode() != 404) {
                throw new TestFailedException("Test Failed");
            }
            conn.getInputStream();
            throw new TestFailedException("Test Failed");
        } catch (TestFailedException e) {
            System.out.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("Pass");
        } finally {
            conn.disconnect();
        }
    }

    private void readAnswerAndCheckId(InputStream in, String expected) throws IOException, TestFailedException {
        String answer = convertInputStreamToString(in);
        validAnswerOrFail(answer, expected);
    }

    private void readAnswerAndCheckUserAgent(InputStream in, String expected) throws IOException, TestFailedException {
        String answer = convertInputStreamToString(in);
        validUserAgentOrFail(answer, expected);
    }

    private void validAnswerOrFail(String jsonPayload, String expected) throws TestFailedException {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);

            JSONObject args = jsonObject.optJSONObject("args");
            JSONObject data = jsonObject.optJSONObject("data");

            String id = "";

            if (args != null && args.has("id")) {
                id = args.getString("id");
            } else if (data != null && data.has("id")) {
                id = data.getString("id");
            }

            if (!id.equals(expected)) throw new TestFailedException("Test failed");

        } catch (JSONException e) {
            throw new TestFailedException("Test failed due to a JSON error: " + e.getMessage(), e);
        }
    }

    private void validUserAgentOrFail(String jsonPayload, String expected) throws TestFailedException {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject headers = jsonObject.getJSONObject("headers");
            String userAgent = headers.getString("user-agent");
            if (!userAgent.equals(expected)) throw new TestFailedException("Test failed");

        } catch (JSONException e) {
            throw new TestFailedException("Test failed due to a JSON error: " + e.getMessage(), e);
        }
    }

    private String makeJsonPayload(String id) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("id", id);
        return jsonObj.toString();
    }

    private String convertInputStreamToString(InputStream is) throws IOException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;

        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString("utf-8");
    }
}
