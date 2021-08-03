package com.trackingplan.examples.urlconnection;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;

import com.trackingplan.client.sdk.Trackingplan;
import com.trackingplan.examples.urlconnection.utils.StringUtils;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        /*
        Map<String, String> customDomains = new HashMap<String, String>() {{
            put("postman-echo.com", "sarge-test");
            put("reqres.in", "sarge-test");
        }};
        */

        Trackingplan.init("TP344383")
                //.sourceAlias("Android Example")
                .environment("PRODUCTION")
                // .ignoreContext()
                // .customDomains(customDomains)
                .enableDebug()
                // .dryRun()
                .start(this);

        new Thread(new RandomGaEventsGenerator()).start();
        // new Thread(new TestGaEvents()).start();
        // new Thread(new TestUrlConnection()).start();
    }

    private static class RandomGaEventsGenerator implements Runnable {
        @Override
        public void run() {

            boolean finish = false;

            Log.i("AppExample", "Start");

            while (!finish) {

                Log.i("AppExample", "Run");

                try {
                    long numRequests = getRandomNumber(4, 12);
                    sendGaRandomRequests(numRequests, 0, 3000);
                } catch (IOException | InterruptedException ex) {
                    Log.v("AppExample", "Error: " + ex.getMessage());
                }

                try {
                    long waitTimeMs = getRandomNumber(0, 20000);
                    Thread.sleep(waitTimeMs);
                } catch (InterruptedException ex) {
                    Log.v("AppExample", "Interrupted: " + ex.getMessage());
                    finish = true;
                }
            }

            Log.i("AppExample", "Finish");
        }

        private void sendGaRandomRequests(long numRequests, long minTimeBetweenMs, long maxTimeBetweenMs) throws IOException, InterruptedException {

            for (int i = 0; i < numRequests; i++) {

                String eventName = "Random" + StringUtils.getRandomAlphaNumericString(10);
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

                long waitTimeMs = getRandomNumber(minTimeBetweenMs, maxTimeBetweenMs);
                Thread.sleep(waitTimeMs);
            }
        }
    }

    private static class TestGaEvents implements Runnable {
        @Override
        public void run() {
            try {
                runGAGetTests();
                runGAPostTests();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void runGAGetTests() throws IOException {

            // TODO: Load test data instead of copying here
            String[] gaRawURLs = new String[]{
                    // ga_get_enhanced_ecom
                    "https://www.google-analytics.com/j/collect?v=1&_v=j88&a=1620142770&t=event&ni=1&cu=EUR&ds=GTM%3A%20GTM-NC7VC6W%2F%2F153%20(debug%3Afalse)&_s=1&dl=https%3A%2F%2Fwww.freepik.es%2Fprofile%2Fpreagreement%2Fgetstarted%2F12857694&dp=%2Fprofile%2Fpreagreement%2Fgetstarted%2F12857694&ul=es-es&de=UTF-8&dt=Freepik%20Premium%3A%20obt%C3%A9n%20todos%20nuestros%20recursos%20y%20archivos%20premium%20por%209%2C99%20%24%2Fmes&sd=24-bit&sr=1200x1920&vp=573x1819&je=0&fl=ikaue-datos-admitidos&ec=ecommerce&ea=ecommerce-impressions&el=ecommerce-impressions&_u=SCCACEALRAAAAG~&jid=1697123033&gjid=1000038941&cid=682785364.1616338781&tid=UA-19303147-22&_gid=1847086507.1616338781&_r=1&gtm=2wg3h0NC7VC6W&cg1=profile&cg2=subscription&cg3=nn&cg4=product-page&cd1=200-ok&cd2=es&cd3=nn&cd4=anonymous&cd5=nn&cd6=not-logged&cd7=nn%2Fnn%2Fnn&cd9=ns&cd10=ns&cd11=ns&cd14=product-page&cm1=0&cm2=0&il1nm=getStarted&il1pi1nm=monthly&il1pi1id=FR-PREMIUM-1-MONTHLY&il1pi1pr=9.99&il1pi1va=new&il1pi1br=nn&il1pi1qt=1&il1pi1ps=1&il1pi1ca=anonymous%2Fprofile%2Fproduct-page&il1pi1cc=nn&il1pi2nm=annual&il1pi2id=FR-PREMIUM-1-ANNUAL&il1pi2pr=89.99&il1pi2va=new&il1pi2br=nn&il1pi2qt=1&il1pi2ps=2&il1pi2ca=anonymous%2Fprofile%2Fproduct-page&il1pi2cc=nn&z=2058738731",
                    // ga_get_event
                    "https://www.google-analytics.com/collect?v=1&_v=j81&a=1079976052&t=event&_s=4&dl=https%3A%2F%2Fdice.fm%2Fpartner%2Fstream-via-la%2Fevent%2Fvw6b6-room-service-24th-apr-stream-via-la-los-angeles-tickets%3F_branch_match_id%3D469406861761518425&dr=https%3A%2F%2Fwww.roomservicefest.com%2F&ul=en&de=UTF-8&dt=ROOM%20SERVICE%20Tickets%20%7C%20Free%20%7C%2024%20Apr%202020%20%40%20Youtube%20%7C%20DICE&sd=24-bit&sr=2560x1080&vp=1691x709&je=0&ec=All&ea=Ticket%20Selection%20Screen&_u=aGBAAEIJ~&jid=&gjid=&cid=1438716784.1587727392&uid=L4Zyw7Qf3mHk0OFDR%2FVFrkwMJ2w%3D&tid=UA-49561032-1&_gid=803700221.1587727392&z=836139612",
                    // ga_get_item
                    "https://www.google-analytics.com/collect?v=1&_v=j82&a=1391801276&t=item&_s=3&dl=https%3A%2F%2Fwww.spartoo.es%2Fcheckout_success.php&dr=https%3A%2F%2Fsas.redsys.es%2Fsas%2FSerSvlSecureAutentica%3Bjsessionid%3D0000nDaC_N534Mcfgc8uhYO_UVW%3A1ccv7n47o&ul=es-es&de=UTF-8&dt=SPARTOO.ES%2C%20%C2%A1pedido%20confirmado!&sd=24-bit&sr=1920x1080&vp=1903x947&je=0&_u=SCCAAAATAAAAg~&jid=&gjid=&cid=200010364.1590080236&uid=19041651&tid=UA-1265644-7&_gid=638039245.1590080236&cd1=0&cd2=0&cd3=1&cd4=0&cd5=0&cd6=0&cd7=0&cd8=0&cd9=0&cd10=0&cd11=0&cd12=0&cd13=0&cd14=0&cd15=0&cd16=0&cd17=0&cd18=0&cd19=0&cd20=0&ti=2000030011687828&in=Mobils%20By%20Mephisto-VALDEN-Cuero%20marr%C3%B3n-46%20&ic=VALDEN&iv=Mobils%20By%20Mephisto&ip=125.62&iq=1&z=1842788745",
                    // ga_get_pageview
                    "https://www.google-analytics.com/collect?v=1&_v=j81&a=1834713686&t=pageview&_s=1&dl=https%3A%2F%2Fgethooksapp.com%2F&ul=en-us&de=UTF-8&dt=Hooks%20-%20Stay%20up%20to%20date%20on%20anything%20and%20chat%20with%20awesome%20people&sd=24-bit&sr=800x600&vp=1351x768&je=0&_u=YGBAgEAB~&jid=1092188148&gjid=978604332&cid=504685930.1587645230&tid=UA-55436059-1&_gid=1478453425.1587645230&z=711644651",
                    // ga_get_timing
                    "https://www.google-analytics.com/collect?v=1&_v=j81&a=1964670952&t=timing&_s=2&dl=https%3A%2F%2Fsendbird.com%2F%3F--%26utm_source%3Dgoogle%26utm_medium%3Dcpc%26utm_campaign%3DBranded-WS-EMEA%26utm_content%3Dhomepage%26utm_term%3Dsendbird%26gclid%3DCjwKCAjw1v_0BRAkEiwALFkj5mo0yI3AvYA1hdM3s9NJA_kMa6Mv4bPZE-d35J6ZbGRDSreS4HT0uRoC5dkQAvD_BwE&dr=https%3A%2F%2Fwww.google.com%2F&ul=en&de=UTF-8&dt=Sendbird%20-%20A%20Complete%20Chat%20Platform%2C%20Messaging%20and%20Chat%20SDK%20and%20API&sd=30-bit&sr=1440x900&vp=833x709&je=0&plt=2291&pdt=4&dns=0&rrt=62&srt=738&tcp=0&dit=1676&clt=1688&_gst=1040&_gbt=1794&_cst=1020&_cbt=1838&_u=aADAAEABAAAAg~&jid=&gjid=&cid=1902727653.1587113689&tid=UA-39104662-14&_gid=671360067.1587600121&gtm=2wg4f0P783HJQ&z=851988578",
                    // ga_get_transaction
                    "https://www.google-analytics.com/collect?v=1&_v=j82&a=1391801276&t=transaction&_s=2&dl=https%3A%2F%2Fwww.spartoo.es%2Fcheckout_success.php&dr=https%3A%2F%2Fsas.redsys.es%2Fsas%2FSerSvlSecureAutentica%3Bjsessionid%3D0000nDaC_N534Mcfgc8uhYO_UVW%3A1ccv7n47o&ul=es-es&de=UTF-8&dt=SPARTOO.ES%2C%20%C2%A1pedido%20confirmado!&sd=24-bit&sr=1920x1080&vp=1903x947&je=0&_u=SCCAAAATAAAAg~&jid=&gjid=&cid=200010364.1590080236&uid=19041651&tid=UA-1265644-7&_gid=638039245.1590080236&cd1=0&cd2=0&cd3=1&cd4=0&cd5=0&cd6=0&cd7=0&cd8=0&cd9=0&cd10=0&cd11=0&cd12=0&cd13=0&cd14=0&cd15=0&cd16=0&cd17=0&cd18=0&cd19=0&cd20=0&ti=2000030011687828&tr=125.62&ts=1.03&tt=26.38&z=688015661",
            };

            for (String rawURL : gaRawURLs) {
                URL url = new URL(rawURL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    conn.getResponseCode();
                } finally {
                    conn.disconnect();
                }
            }
        }

        private void runGAPostTests() throws IOException {

            // TODO: Load test data instead of copying here
            ArrayList<Pair<String, String>> testItems = new ArrayList<Pair<String, String>>() {{
                // ga_post_event_batch
                add(new Pair<>(
                        "https://ssl.google-analytics.com/batch",
                        "ea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=343&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.neoyL&av=1.0&_crc=0&ht=1625729421711&qt=120043&z=123098366502417343%@\\nea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=344&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.oyL&av=1.0&_crc=0&ht=1625729430294&qt=111460&z=123098366502417344%@\\nea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=345&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.oyL&av=1.0&_crc=0&ht=1625729432577&qt=109178&z=123098366502417345%@\\nea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=346&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.oyL&av=1.0&_crc=0&ht=1625729435212&qt=106542&z=123098366502417346%@\\nea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=347&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.oyL&av=1.0&_crc=0&ht=1625729438432&qt=103323&z=123098366502417347%@\\nea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=348&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.oyL&av=1.0&_crc=0&ht=1625729462729&qt=79026&z=123098366502417348%@\\nea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=349&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.oyL&av=1.0&_crc=0&ht=1625729466859&qt=74896&z=123098366502417349%@\\nea=Start+Pressed&ul=en&_v=mi3.1.7&an=Reddit&dm=x86_64&a=588695299&el=Start+Timer+One&ec=Game+1&_s=350&ds=app&aid=com.carsonkatri.Reddit&sr=1125x2436&t=event&tid=UA-163365060-1&v=1&cid=7ef88044-f7f0-46cb-96fc-be8530bca874&_u=.oyL&av=1.0&_crc=0&ht=1625729469942&qt=71813&z=123098366502417350"
                ));
                // ga_post_event
                add(new Pair<>(
                        "https://www.google-analytics.com/r/collect",
                        "v=1&_v=j81&a=953021710&t=event&ni=1&_s=1&dl=https%3A%2F%2Funity.com%2F&dr=https%3A%2F%2Fwww.intercom.com%2Fcustomers%2Funity&dp=%2F&ul=en&de=UTF-8&dt=Unity%20Real-Time%20Development%20Platform%20%7C%203D%2C%202D%20VR%20%26%20AR%20Visualizations&sd=24-bit&sr=2560x1080&vp=1335x709&je=0&ec=scroll%20depth&el=100&_u=aGDACEALR~&jid=960160269&gjid=1015578035&cid=995732072.1587600203&tid=UA-2854981-61&_gid=1384444010.1587714237&_r=1&gtm=2wg4f05V25JL6&cd1=&cd2=No&cd14=https%3A%2F%2Fwww.intercom.com%2Fcustomers%2Funity&cd18=GTM-5V25JL6&cd19=113&cd21=www.intercom.com&cd22=%2F&cd45=&cd20=995732072.1587600203&z=994029658"
                ));
            }};

            for (Pair<String, String> testItem : testItems) {

                URL url = new URL(testItem.first);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setupJsonConnection(conn);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                try (OutputStream out = conn.getOutputStream()) {
                    byte[] buffer = testItem.second.getBytes(StandardCharsets.UTF_8);
                    out.write(buffer, 0, buffer.length);
                }

                try {
                    conn.getResponseCode();
                } finally {
                    conn.disconnect();
                }
            }
        }
    }

    private static class TestUrlConnection implements Runnable {

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
            setupJsonConnection(conn);

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
            setupJsonConnection(conn);

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
            setupJsonConnection(conn);

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
            setupJsonConnection(conn);

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
            setupJsonConnection(conn);

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
            setupJsonConnection(conn);
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
            setupJsonConnection(conn);
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
            setupJsonConnection(conn);

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
            setupJsonConnection(conn);

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
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int length;

            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString("utf-8");
        }
    }

    private static class TestFailedException extends Exception {
        public TestFailedException(String message) {
            super(message);
        }
        public TestFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static void setupJsonConnection(HttpURLConnection conn) {
        conn.setRequestProperty("Accept", "application/json");
    }

    private static long getRandomNumber(long min, long max) {
        return (long) ((Math.random() * (max - min)) + min);
    }
}
