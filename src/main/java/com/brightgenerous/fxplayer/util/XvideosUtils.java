package com.brightgenerous.fxplayer.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XvideosUtils {

    private static final Pattern patternTitle = Pattern.compile("<title>([^<]*)</title>");

    private static final Pattern patternFlashVars = Pattern.compile("flashvars=\"(.*?)?\"");

    private static final Pattern patternUrl = Pattern.compile("flv_url=(.*?)(?:&.*|)$");

    private XvideosUtils() {
    }

    public static boolean isVideoUrl(String url) {
        if (url == null) {
            return false;
        }

        try {
            URL _url = new URL(url);
            String host = _url.getHost();
            if (host == null) {
                return false;
            }
            if (host.indexOf("xvideos.com") < 0) {
                return false;
            }
            return true;
        } catch (MalformedURLException e) {
        }

        return false;
    }

    public static String extractTitle(String html) {
        if (html == null) {
            return null;
        }

        Matcher matcher = patternTitle.matcher(html);
        if (matcher.find()) {
            String title = matcher.group(1);
            if (title != null) {
                title = title.trim();
            }
            return title;
        }
        return null;
    }

    public static String extractUrlSafely(String url) {
        return extractUrl(url, null);
    }

    public static String extractUrl(String url, ExceptionHandler<Exception> handler) {
        String ret = null;
        try {
            ret = extractUrl(url);
        } catch (IOException e) {
            if (handler != null) {
                handler.handle(e);
            }
        }
        return ret;
    }

    public static String extractUrl(String url) throws IOException {
        String html = getPageHtml(url);
        if (html == null) {
            return null;
        }

        Matcher mFlashVars = patternFlashVars.matcher(html);
        String flashVars = null;
        if (mFlashVars.find()) {
            flashVars = mFlashVars.group(1);
        }

        if (flashVars == null) {
            return null;
        }

        Matcher mUrl = patternUrl.matcher(flashVars);
        String _url = null;
        if (mUrl.find()) {
            _url = mUrl.group(1);
        }

        if (_url == null) {
            return null;
        }

        return URLDecoder.decode(_url, "UTF-8");
    }

    private static String getPageHtml(String url) throws IOException {
        if (url == null) {
            return null;
        }

        {
            int idx = url.indexOf('#');
            if (0 <= idx) {
                url = url.substring(0, idx);
            }
        }
        {
            int idx = url.indexOf('?');
            if (0 <= idx) {
                url = url.substring(0, idx);
            }
        }
        {
            int idx = url.indexOf('&');
            if (0 <= idx) {
                url = url.substring(0, idx);
            }
        }

        return HttpUtils.execGet(url, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0.1)",
                Charset.forName("UTF-8"));
    }
}
