package com.redhat.jenkins.plugins.buildrequester;

import hudson.model.Failure;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

/**
 * @author vdedik@redhat.com
 */
public class Utils {
    public static URL normalize(URL url) throws MalformedURLException {
        String stringUrl = url.toString();
        if (!"/".equals(stringUrl.substring(stringUrl.length() - 1))) {
            stringUrl += "/";
        }
        return new URL(stringUrl);
    }

    public static String get(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        for (String headerName : headers.keySet()) {
            conn.setRequestProperty(headerName, headers.get(headerName));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode / 100 != 2) {
            throw new Failure("Failed to send the request: " + conn.getResponseMessage());
        }

        InputStream response = conn.getInputStream();
        String content = convertStreamToString(response);
        response.close();

        return content;
    }

    public static String post(URL url, String data, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        for (String headerName : headers.keySet()) {
            conn.setRequestProperty(headerName, headers.get(headerName));
        }
        conn.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        if (data != null) {
            wr.writeBytes(data);
        }
        wr.flush();
        wr.close();

        int responseCode = conn.getResponseCode();
        if (responseCode / 100 != 2) {
            throw new Failure("Failed to send the request: " + conn.getResponseMessage());
        }

        InputStream response = conn.getInputStream();
        String content = convertStreamToString(response);
        response.close();

        return content;
    }

    public static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
