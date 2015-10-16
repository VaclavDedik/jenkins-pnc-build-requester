package com.redhat.jenkins.plugins.buildrequester;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public class HttpUtils {

    public static Response get(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        for (String headerName : headers.keySet()) {
            conn.setRequestProperty(headerName, headers.get(headerName));
        }

        Response response = new Response();
        response.setResponseCode(conn.getResponseCode());
        response.setContent(readContent(conn));
        response.setResponseMessage(conn.getResponseMessage());

        return response;
    }

    public static Response post(URL url, String data, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
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

        Response response = new Response();
        response.setResponseCode(conn.getResponseCode());
        response.setContent(readContent(conn));
        response.setResponseMessage(conn.getResponseMessage());

        return response;
    }

    public static class Response {
        private Integer responseCode;
        private String content;
        private String responseMessage;

        public Integer getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(Integer responseCode) {
            this.responseCode = responseCode;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public void setResponseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
        }
    }

    private static String readContent(HttpURLConnection conn) throws IOException{
        String content = "";
        try {
            InputStream responseContent = conn.getInputStream();
            if (responseContent != null) {
                content = Utils.convertStreamToString(responseContent);
                responseContent.close();
            }
        } catch (IOException e) {
            InputStream responseErrorContent = conn.getErrorStream();
            if (responseErrorContent != null) {
                content = Utils.convertStreamToString(responseErrorContent);
                responseErrorContent.close();
            }
        }

        return content;
    }
}
