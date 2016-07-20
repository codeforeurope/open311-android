package org.open311.android.network;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import org.codeforamerica.open311.facade.Format;
import org.codeforamerica.open311.internals.network.NetworkManager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @copyright 2016 City of Eindhoven, Netherlands
 * @license http://www.gnu.org/licenses/gpl.txt GNU/GPL, see LICENSE.txt
 * @author Bas Biezemans
 */
public class MultipartHTTPNetworkManager implements NetworkManager {
    private static final String LOG_TAG = "MultipartHTTPNetworkM..";
    private static final int TIMEOUT = 5000;
    private static final String FILENAME = "media.jpg";
    private Bitmap bitmap;

    public MultipartHTTPNetworkManager(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    /**
     * Sends a GET HTTP request.
     *
     * @param url Target.
     * @return Server response.
     * @throws IOException If there was any problem with the connection.
     */
    @Override
    public String doGet(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String response = null;
        try {
            response = getResponseAsString(conn);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }
        return response;
    }

    @Override
    public String doPost(URL url, Map<String, String> parameters) throws IOException {

        // Fix for HTTP connection reuse which was buggy pre-froyo
        disableConnectionReuseIfNecessary();

        // Create a unique boundary
        final String boundary = "------------------" + System.currentTimeMillis();

        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        entity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entity.setCharset(Charset.forName("UTF-8"));
        entity.setBoundary(boundary);

        for (Entry<String, String> entry : parameters.entrySet()) {
            entity.addTextBody(entry.getKey(), entry.getValue());
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ContentType contentType = ContentType.create("image/jpg");
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] binaryData = stream.toByteArray();
        entity.addBinaryBody("media", binaryData, contentType, FILENAME);

        HttpEntity httpEntity = entity.build();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Debug
        Log.d(LOG_TAG, "EndPoint URL: " + url.toString());

        conn.setDoOutput(true); // POST
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setChunkedStreamingMode(0); // 0 -> use default chunk size
        conn.setConnectTimeout(TIMEOUT);
        //conn.setReadTimeout(TIMEOUT); // 10000

        // Request header fields
        conn.setRequestProperty("Accept-Language", Locale.getDefault().toString().replace("_", "-"));
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.addRequestProperty("Content-length", String.valueOf(httpEntity.getContentLength()));
        try {
            httpEntity.writeTo(conn.getOutputStream());
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return getResponseAsString(conn);
            } else {
                return conn.getResponseMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Sets the desired format of the requests.
     *
     * @param format A serialization format (XML or JSON).
     */
    @Override
    public void setFormat(Format format) { }

    private void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private String getResponseAsString(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
