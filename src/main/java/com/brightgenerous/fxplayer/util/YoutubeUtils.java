package com.brightgenerous.fxplayer.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUtils {

    public static class VideoInfo {

        private final String url;

        private String title;

        VideoInfo(String url, String title) {
            this.url = url;
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        void setTitle(String title) {
            this.title = title;
        }
    }

    private static final String URL_HOST = "http://www.youtube.com";

    private YoutubeUtils() {
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
            if (host.indexOf("youtube.com") < 0) {
                return false;
            }
            String path = _url.getPath();
            if (path == null) {
                return false;
            }
            return path.startsWith("/watch");
        } catch (MalformedURLException e) {
        }

        return false;
    }

    public static boolean isPlaylistUrl(String url) {
        if (url == null) {
            return false;
        }

        try {
            URL _url = new URL(url);
            String host = _url.getHost();
            if (host == null) {
                return false;
            }
            if (host.indexOf("youtube.com") < 0) {
                return false;
            }
            String path = _url.getPath();
            if (path == null) {
                return false;
            }
            return path.startsWith("/playlist") || path.startsWith("/channel")
                    || path.startsWith("/user") || path.startsWith("/results");
        } catch (MalformedURLException e) {
        }

        return false;
    }

    private static final Pattern anchor = Pattern.compile("<a"
            + "(?:[^>]*\\stitle\\s*=\\s*\"([^\"]*)\")?"
            // [x] keep only first argument
            + "[^>]*\\shref\\s*=\\s*\"(/watch[^&\"]*)[^\"]*\""
            + "(?:[^>]*\\stitle\\s*=\\s*\"([^\"]*)\")?" + "[^>]*>");

    public static List<VideoInfo> parsePlaylist(String html) {
        if (html == null) {
            return null;
        }

        List<VideoInfo> ret = new ArrayList<>();

        Map<String, VideoInfo> infos = new HashMap<>();
        Matcher matcher = anchor.matcher(html);
        while (matcher.find()) {
            String href = matcher.group(2);
            String title;
            {
                String title1 = matcher.group(1);
                String title2 = matcher.group(3);
                if ((title1 == null) || (title2 == null)) {
                    if (title1 != null) {
                        title = title1;
                    } else if (title2 != null) {
                        title = title2;
                    } else {
                        title = null;
                    }
                } else if (title1.length() < title2.length()) {
                    title = title2;
                } else {
                    title = title1;
                }
                if (title != null) {
                    title = title.replace("&#39;", "'").replace("&quot;", "\"")
                            .replace("&amp;", "&").trim();
                }
            }
            String url = URL_HOST + href;
            VideoInfo info = infos.get(url);
            if (info == null) {
                info = new VideoInfo(url, title);
                infos.put(url, info);
                ret.add(info);
            } else {
                if (info.getTitle() == null) {
                    info.setTitle(title);
                } else if ((title != null) && (info.getTitle().length() < title.length())) {
                    info.setTitle(title);
                }
            }
        }

        return ret;
    }

    private static final Pattern patternTitle = Pattern
            .compile("<span[^>]*\\sid\\s*=\\s*\"eow-title\"[^>]*>([^<]*)</span>");

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
        if (url == null) {
            return null;
        }

        String ret = null;

        List<Video> videos = getStreamingUrisFromUrl(url);
        if ((videos != null) && !videos.isEmpty()) {
            List<Video> tmp = new ArrayList<>(videos);
            Collections.sort(tmp, priorityComparator);
            ret = tmp.get(0).url;
        }

        return ret;
    }

    private static final List<Meta> metas;

    private static final Map<String, Meta> typeMap;

    private static final Comparator<Video> priorityComparator;
    static {
        metas = new ArrayList<>();
        metas.add(new Meta("38", "MP4", "High Quality - 2048x1536"));
        metas.add(new Meta("37", "MP4", "High Quality - 1920x1080"));
        metas.add(new Meta("22", "MP4", "High Quality - 1280x720"));
        metas.add(new Meta("18", "MP4", "Medium Quality - 640x360"));
        metas.add(new Meta("35", "FLV", "High Quality - 854x480"));
        metas.add(new Meta("34", "FLV", "Medium Quality - 640x360"));
        metas.add(new Meta("6", "FLV", "Medium Quality - 640x360"));
        metas.add(new Meta("5", "FLV", "Low Quality - 320x240"));
        metas.add(new Meta("45", "WEBM", "High Quality - 1280x720"));
        metas.add(new Meta("44", "WEBM", "Medium Quality - 854x480"));
        metas.add(new Meta("43", "WEBM", "Low Quality - 640x360"));
        metas.add(new Meta("36", "3GP", "High Quality - 320x240"));
        metas.add(new Meta("17", "3GP", "Medium Quality - 176x144"));
        metas.add(new Meta("13", "3GP", "Low Quality - 176x144"));

        typeMap = new HashMap<>();
        for (Meta meta : metas) {
            typeMap.put(meta.num, meta);
        }

        priorityComparator = new Comparator<Video>() {

            @Override
            public int compare(Video o1, Video o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                int idx1 = metas.indexOf(o1.meta);
                int idx2 = metas.indexOf(o2.meta);
                if (idx1 == idx2) {
                    return 0;
                }
                if (idx1 < 0) {
                    return 1;
                }
                if (idx2 < 0) {
                    return -1;
                }
                return idx1 - idx2;
            }
        };
    }

    private static final Pattern patternStreamMap = Pattern.compile("stream_map\": \"(.*?)?\"");

    private static final Pattern patternItag = Pattern
            .compile("^(?:.*\\?|.*&)?itag=([0-9]+?)(?:&.*)?$");

    private static final Pattern patternSig = Pattern.compile("^(?:.*\\?|.*&)?sig=(.*?)(?:&.*)?$");

    private static final Pattern patternUrl = Pattern.compile("^(?:.*\\?|.*&)?url=(.*?)(?:&.*)?$");

    private static List<Video> getStreamingUrisFromUrl(String url) throws IOException {
        String html = getPageHtml(url);
        if (html == null) {
            return null;
        }

        List<Video> ret = parseAsVideoHtml(html);
        if ((ret == null) || ret.isEmpty()) {
            ret = parseAsConfirmHtml(html);
        }
        return ret;
    }

    private static List<Video> parseAsConfirmHtml(String html) throws IOException {
        List<Video> ret = null;
        String _url = parseUrlAsConfirm(html);
        if (_url != null) {
            String _html = getPageHtml(_url);
            if (_html != null) {
                ret = parseAsVideoHtml(_html);
            }
        }
        return ret;
    }

    private static String parseUrlAsConfirm(String html) throws IOException {
        String ret = null;
        return ret;
    }

    private static List<Video> parseAsVideoHtml(String html) throws IOException {
        if (html == null) {
            return null;
        }

        html = html.replace("\\u0026", "&");

        // Parse the HTML response and extract the streaming URIs
        if (html.contains("verify-age-thumb")) {
            return null;
        }

        if (html.contains("das_captcha")) {
            return null;
        }

        List<String> streamMaps = new ArrayList<>();
        {
            Matcher mStreamMap = patternStreamMap.matcher(html);
            while (mStreamMap.find()) {
                streamMaps.add(mStreamMap.group(1));
            }
        }

        if (streamMaps.isEmpty()) {
            return null;
        }

        Map<String, String> itagUrls = new HashMap<>();
        {
            String items[] = streamMaps.get(0).split(",");
            if ((items != null) && (0 < items.length)) {
                for (String item : items) {
                    String decoded = URLDecoder.decode(item, "UTF-8");

                    String itag = null;
                    {
                        Matcher matcher = patternItag.matcher(decoded);
                        if (matcher.find()) {
                            itag = matcher.group(1);
                        }
                    }

                    String sig = null;
                    {
                        Matcher matcher = patternSig.matcher(decoded);
                        if (matcher.find()) {
                            sig = matcher.group(1);
                        }
                    }

                    String url = null;
                    {
                        Matcher matcher = patternUrl.matcher(item);
                        if (matcher.find()) {
                            url = matcher.group(1);
                        }
                    }

                    if ((itag != null) && (sig != null) && (url != null)) {
                        itagUrls.put(itag, URLDecoder.decode(url, "UTF-8") + "&" + "signature="
                                + sig);
                    }
                }
            }
        }

        if (itagUrls.size() == 0) {
            return null;
        }

        List<Video> videos = new ArrayList<>();

        for (Entry<String, String> entry : itagUrls.entrySet()) {
            String itag = entry.getKey();
            String url = entry.getValue();
            Meta meta = typeMap.get(itag);
            if (meta != null) {
                videos.add(new Video(meta, url));
            }
        }

        return videos;
    }

    private static String getPageHtml(String url) throws IOException {
        if (url == null) {
            return null;
        }

        // [x] keep only first argument
        // [ ] keep only argument "v"

        // Remove any query params in query string after the watch?v=<vid> in
        // e.g.
        // http://www.youtube.com/watch?v=0RUPACpf8Vs&feature=youtube_gdata_player
        {
            int andIdx = url.indexOf('&');
            if (0 <= andIdx) {
                url = url.substring(0, andIdx);
            }
        }

        return HttpUtils.execGet(url, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0.1)",
                Charset.forName("UTF-8"));
    }
}

class Meta {

    public final String num;

    public final String ext;

    public final String type;

    Meta(String num, String ext, String type) {
        this.num = num;
        this.ext = ext;
        this.type = type;
    }
}

class Video {

    public final Meta meta;

    public final String url;

    Video(Meta meta, String url) {
        this.meta = meta;
        this.url = url;
    }
}
