package com.redhat.jenkins.plugins.buildrequester;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

    public static String convertStreamToString(InputStream is) {
        if (is == null) {
            return "";
        }
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
