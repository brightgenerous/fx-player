package com.brightgenerous.fxplayer.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUtils {

    private static final String URL_HOST = "http://www.youtube.com";

    private static final Pattern pagePattern = Pattern.compile("(.*(?:\\?|&)page=)(\\d*)(.*)");

    private YoutubeUtils() {
    }

    public static String getQueryUrl(String word) throws UnsupportedEncodingException {
        if (word == null) {
            return null;
        }
        return URL_HOST + "/results?search_query=" + URLEncoder.encode(word, "UTF-8") + "&sm=3";
    }

    public static String getQueryPageUrl(String url, int inc) {
        if (url == null) {
            return null;
        }
        if (!url.contains("youtube.com/results")) {
            return null;
        }
        String ret = null;
        Matcher matcher = pagePattern.matcher(url);
        if (matcher.find()) {
            String _u1 = matcher.group(1);
            String _p = matcher.group(2);
            String _u2 = matcher.group(3);
            int page = 1;
            try {
                page = Integer.parseInt(_p) + inc;
            } catch (NumberFormatException e) {
            }
            if (0 < page) {
                ret = _u1 + page + _u2;
            }
        } else {
            int page = 1 + inc;
            if (0 < page) {
                if (url.contains("?")) {
                    ret = url + "&page=" + page;
                } else {
                    ret = url + "?page=" + page;
                }
            }
        }
        return ret;
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
            if (!host.contains("youtube.com")) {
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
            if (!host.contains("youtube.com")) {
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
            + "(?:[^>]*\\stitle\\s*=\\s*\"([^\"]*)\")?" + "[^>]*>" + "\\s*([^<]*)\\s*</a>");

    public static List<VideoInfo> parsePlaylist(String html) {
        if (html == null) {
            return null;
        }

        List<VideoInfo> ret = new ArrayList<>();

        Map<String, VideoInfo> infos = new HashMap<>();
        Matcher matcher = anchor.matcher(html);
        while (matcher.find()) {
            boolean notPL = matcher.group().contains("yt-uix-tile-link");
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
                {
                    String title3 = matcher.group(4);
                    if (title3 != null) {
                        if (title == null) {
                            title = title3;
                        } else if (title.length() < title3.length()) {
                            title = title3;
                        }
                    }
                }
                if (title != null) {
                    title = title.replace("&#39;", "'").replace("&quot;", "\"")
                            .replace("&amp;", "&").trim();
                }
            }
            String url = URL_HOST + href;
            VideoInfo info = infos.get(url);
            if (info == null) {
                if (notPL) {
                    info = new VideoInfo(url, title);
                    infos.put(url, info);
                    ret.add(info);
                }
            } else {
                if (info.getTitle() == null) {
                    info.setTitle(title);
                } else if (!notPL && (title != null)) {
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
        metas.add(new Meta("38", "mp4", 4096, 3072));
        metas.add(new Meta("37", "mp4", 1920, 1080));
        metas.add(new Meta("22", "mp4", 1280, 720));
        metas.add(new Meta("18", "mp4", 640, 360));
        metas.add(new Meta("35", "flv", 854, 480));
        metas.add(new Meta("34", "flv", 640, 360));
        metas.add(new Meta("6", "flv", 450, 270));
        metas.add(new Meta("5", "flv", 400, 240));
        metas.add(new Meta("46", "webm", 1920, 1080));
        metas.add(new Meta("45", "webm", 1280, 720));
        metas.add(new Meta("44", "webm", 854, 480));
        metas.add(new Meta("43", "webm", 640, 360));
        metas.add(new Meta("36", "3gp", 320, 240));
        metas.add(new Meta("17", "3gp", 176, 144));
        metas.add(new Meta("13", "3gp"));

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

    private static final Pattern patternStreamMap = Pattern
            .compile("stream_map\"\\s*:\\s*\"([^\"]*)\"");

    private static final Pattern patternItag = Pattern.compile("^(?:.*&)?itag=([^&]*?)(?:&.*)?$");

    private static final Pattern patternSig = Pattern.compile("^(?:.*&)?sig=([^&]*?)(?:&.*)?$");

    private static final Pattern patternS = Pattern.compile("^(?:.*&)?s=([^&]*?)(?:&.*)?$");

    private static final Pattern patternUrl = Pattern.compile("^(?:.*&)?url=([^&]*?)(?:&.*)?$");

    private static final Pattern patternRatebypass = Pattern
            .compile("^.*(?:\\?|&)ratebypass=([^&]*?)(?:&.*)?$");

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

        if (html.contains("verify-age-thumb")) {
            return null;
        }

        if (html.contains("das_captcha")) {
            return null;
        }

        html = html.replace("\\u0026", "&");

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
                    String itag = null;
                    {
                        Matcher matcher = patternItag.matcher(item);
                        if (matcher.find()) {
                            itag = matcher.group(1);
                        }
                        if (itag == null) {
                            continue;
                        }
                    }

                    String sig = null;
                    {
                        {
                            Matcher matcher = patternSig.matcher(item);
                            if (matcher.find()) {
                                sig = matcher.group(1);
                            }
                        }
                        if (sig == null) {
                            if (false) {
                                Matcher matcher = patternS.matcher(item);
                                if (matcher.find()) {
                                    sig = convS2Sig(matcher.group(1));
                                }
                            }
                        }
                        if (sig == null) {
                            continue;
                        }
                    }

                    String url = null;
                    {
                        Matcher matcher = patternUrl.matcher(item);
                        if (matcher.find()) {
                            url = matcher.group(1);
                        }
                        if (url == null) {
                            continue;
                        }
                    }

                    String _url = URLDecoder.decode(url, "UTF-8");
                    if (!patternRatebypass.matcher(_url).find()) {
                        _url += "&ratebypass=yes";
                    }
                    itagUrls.put(itag, _url + "&signature=" + sig);
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

    private static String convS2Sig(String str) {
        return convS2Sig(str, false);
    }

    private static String convS2Sig(String str, boolean age_gate) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(str.length());
        if (age_gate && (str.length() == 86)) {
            // s[2:63] + s[82] + s[64:82] + s[63]
            sb.append(str.substring(2, 63)).append(str.charAt(82)).append(str.substring(64, 82))
                    .append(str.charAt(63));
        }
        if (sb.length() < 1) {
            switch (str.length()) {
                case 93:
                    // s[86:29:-1] + s[88] + s[28:5:-1]
                    sb.append(new StringBuilder(str.substring(30, 87)).reverse())
                            .append(str.charAt(88))
                            .append(new StringBuilder(str.substring(6, 29)).reverse());
                    break;
                case 92:
                    // s[25] + s[3:25] + s[0] + s[26:42] + s[79] + s[43:79] + s[91] + s[80:83]
                    sb.append(str.charAt(25)).append(str.substring(3, 25)).append(str.charAt(0))
                            .append(str.substring(26, 42)).append(str.charAt(79))
                            .append(str.substring(43, 79)).append(str.charAt(91))
                            .append(str.substring(80, 83));
                    break;
                case 91:
                    // s[84:27:-1] + s[86] + s[26:5:-1]
                    sb.append(new StringBuilder(str.substring(28, 85)).reverse())
                            .append(str.charAt(86))
                            .append(new StringBuilder(str.substring(6, 27)).reverse());
                    break;
                case 90:
                    // s[25] + s[3:25] + s[2] + s[26:40] + s[77] + s[41:77] + s[89] + s[78:81]
                    sb.append(str.charAt(25)).append(str.substring(3, 25)).append(str.charAt(2))
                            .append(str.substring(26, 40)).append(str.charAt(77))
                            .append(str.substring(41, 77)).append(str.charAt(89))
                            .append(str.substring(78, 81));
                    break;
                case 89:
                    // s[84:78:-1] + s[87] + s[77:60:-1] + s[0] + s[59:3:-1]
                    sb.append(new StringBuilder(str.substring(79, 85)).reverse())
                            .append(str.charAt(87))
                            .append(new StringBuilder(str.substring(61, 78)).reverse())
                            .append(str.charAt(0))
                            .append(new StringBuilder(str.substring(4, 60)).reverse());
                    break;
                case 88:
                    // s[7:28] + s[87] + s[29:45] + s[55] + s[46:55] + s[2] + s[56:87] + s[28]
                    sb.append(str.substring(7, 28)).append(str.charAt(87))
                            .append(str.substring(29, 45)).append(str.charAt(55))
                            .append(str.substring(46, 55)).append(str.charAt(2))
                            .append(str.substring(56, 87)).append(str.charAt(28));
                    break;
                case 87:
                    // s[6:27] + s[4] + s[28:39] + s[27] + s[40:59] + s[2] + s[60:]
                    sb.append(str.substring(6, 27)).append(str.charAt(4))
                            .append(str.substring(28, 39)).append(str.charAt(27))
                            .append(str.substring(40, 59)).append(str.charAt(2))
                            .append(str.substring(60));
                    break;
                case 86:
                    // s[80:72:-1] + s[16] + s[71:39:-1] + s[72] + s[38:16:-1] + s[82] + s[15::-1]
                    sb.append(new StringBuilder(str.substring(73, 81)).reverse())
                            .append(str.charAt(16))
                            .append(new StringBuilder(str.substring(40, 72)).reverse())
                            .append(str.charAt(72))
                            .append(new StringBuilder(str.substring(17, 39)).reverse())
                            .append(str.charAt(82))
                            .append(new StringBuilder(str.substring(15)).reverse());
                    break;
                case 85:
                    // s[3:11] + s[0] + s[12:55] + s[84] + s[56:84]
                    sb.append(str.substring(3, 11)).append(str.charAt(0))
                            .append(str.substring(12, 55)).append(str.charAt(84))
                            .append(str.substring(56, 84));
                    break;
                case 84:
                    // s[78:70:-1] + s[14] + s[69:37:-1] + s[70] + s[36:14:-1] + s[80] + s[:14][::-1]
                    sb.append(new StringBuilder(str.substring(71, 79)).reverse())
                            .append(str.charAt(14))
                            .append(new StringBuilder(str.substring(38, 70)).reverse())
                            .append(str.charAt(70))
                            .append(new StringBuilder(str.substring(15, 37)).reverse())
                            .append(str.charAt(80))
                            .append(new StringBuffer(str.substring(0, 14)).reverse());
                    break;
                case 83:
                    // s[80:63:-1] + s[0] + s[62:0:-1] + s[63]
                    sb.append(new StringBuilder(str.substring(64, 81)).reverse())
                            .append(str.charAt(0))
                            .append(new StringBuilder(str.substring(1, 63)).reverse())
                            .append(str.charAt(63));
                    break;
                case 82:
                    // s[80:37:-1] + s[7] + s[36:7:-1] + s[0] + s[6:0:-1] + s[37]
                    sb.append(new StringBuilder(str.substring(38, 81)).reverse())
                            .append(str.charAt(7))
                            .append(new StringBuilder(str.substring(8, 37)).reverse())
                            .append(str.charAt(0))
                            .append(new StringBuilder(str.substring(1, 7)).reverse())
                            .append(str.charAt(37));
                    break;
                case 81:
                    // s[56] + s[79:56:-1] + s[41] + s[55:41:-1] + s[80] + s[40:34:-1] + s[0] + s[33:29:-1] + s[34] + s[28:9:-1] + s[29] + s[8:0:-1] + s[9]
                    sb.append(str.charAt(56))
                            .append(new StringBuilder(str.substring(57, 80)).reverse())
                            .append(str.charAt(41))
                            .append(new StringBuilder(str.substring(42, 56)).reverse())
                            .append(str.charAt(80))
                            .append(new StringBuilder(str.substring(35, 41)).reverse())
                            .append(str.charAt(0))
                            .append(new StringBuffer(str.substring(30, 34)).reverse())
                            .append(str.charAt(34))
                            .append(new StringBuffer(str.substring(10, 29)).reverse())
                            .append(str.charAt(29))
                            .append(new StringBuffer(str.substring(1, 9)).reverse())
                            .append(str.charAt(9));
                    break;
                case 80:
                    // s[1:19] + s[0] + s[20:68] + s[19] + s[69:80]
                    sb.append(str.substring(1, 19)).append(str.charAt(0))
                            .append(str.substring(20, 68)).append(str.charAt(19))
                            .append(str.substring(69, 80));
                    break;
                case 79:
                    // s[54] + s[77:54:-1] + s[39] + s[53:39:-1] + s[78] + s[38:34:-1] + s[0] + s[33:29:-1] + s[34] + s[28:9:-1] + s[29] + s[8:0:-1] + s[9]
                    sb.append(str.charAt(54))
                            .append(new StringBuilder(str.substring(55, 78)).reverse())
                            .append(str.charAt(39))
                            .append(new StringBuilder(str.substring(40, 54)).reverse())
                            .append(str.charAt(78))
                            .append(new StringBuilder(str.substring(35, 39)).reverse())
                            .append(str.charAt(0))
                            .append(new StringBuffer(str.substring(30, 34)).reverse())
                            .append(str.charAt(34))
                            .append(new StringBuffer(str.substring(10, 29)).reverse())
                            .append(str.charAt(29))
                            .append(new StringBuffer(str.substring(1, 9)).reverse())
                            .append(str.charAt(9));
                    break;
            }
        }
        return (0 < sb.length()) ? sb.toString() : null;
    }

    private static String _convS2Sig(String str) {
        if ((str == null) || (str.length() < 52)) {
            return null;
        }
        List<Character> chars = new LinkedList<>();
        for (char c : str.toCharArray()) {
            chars.add(Character.valueOf(c));
        }
        chars = chars.subList(2, chars.size());
        Collections.reverse(chars);
        chars = chars.subList(3, chars.size());
        convS2Sig_sub(chars, 9);
        chars = chars.subList(3, chars.size());
        convS2Sig_sub(chars, 43);
        chars = chars.subList(3, chars.size());
        Collections.reverse(chars);
        convS2Sig_sub(chars, 23);
        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static void convS2Sig_sub(List<Character> chars, int i) {
        Character ci = chars.get(i % chars.size());
        Character c0 = chars.remove(0);
        chars.add(0, ci);
        chars.remove(i);
        chars.add(i, c0);
    }
}

class Meta {

    public final String num;

    public final String ext;

    public final int width;

    public final int height;

    Meta(String num, String ext) {
        this(num, ext, -1, -1);
    }

    Meta(String num, String ext, int width, int height) {
        this.num = num;
        this.ext = ext;
        this.width = width;
        this.height = height;
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
