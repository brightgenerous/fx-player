package com.brightgenerous.fxplayer.util;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUtils {

    private static final Map<String, Meta> typeMap = new HashMap<>();
    static {
        typeMap.put("13", new Meta("13", "3GP", "Low Quality - 176x144"));
        typeMap.put("17", new Meta("17", "3GP", "Medium Quality - 176x144"));
        typeMap.put("36", new Meta("36", "3GP", "High Quality - 320x240"));
        typeMap.put("5", new Meta("5", "FLV", "Low Quality - 400x226"));
        typeMap.put("6", new Meta("6", "FLV", "Medium Quality - 640x360"));
        typeMap.put("34", new Meta("34", "FLV", "Medium Quality - 640x360"));
        typeMap.put("35", new Meta("35", "FLV", "High Quality - 854x480"));
        typeMap.put("43", new Meta("43", "WEBM", "Low Quality - 640x360"));
        typeMap.put("44", new Meta("44", "WEBM", "Medium Quality - 854x480"));
        typeMap.put("45", new Meta("45", "WEBM", "High Quality - 1280x720"));
        typeMap.put("18", new Meta("18", "MP4", "Medium Quality - 480x360"));
        typeMap.put("22", new Meta("22", "MP4", "High Quality - 1280x720"));
        typeMap.put("37", new Meta("37", "MP4", "High Quality - 1920x1080"));
        typeMap.put("33", new Meta("38", "MP4", "High Quality - 4096x230"));
    }

    private YoutubeUtils() {
    }

    public static String resolveFileUrl(String url) {
        String ret = null;
        try {
            List<Video> videos = getStreamingUrisFromYouTubePage(url);
            if ((videos != null) && !videos.isEmpty()) {
                String retVidUrl = null;
                for (Video video : videos) {
                    if (video.ext.toLowerCase().contains("mp4")
                            && video.type.toLowerCase().contains("medium")) {
                        retVidUrl = video.url;
                        break;
                    }
                }
                if (retVidUrl == null) {
                    for (Video video : videos) {
                        if (video.ext.toLowerCase().contains("3gp")
                                && video.type.toLowerCase().contains("medium")) {
                            retVidUrl = video.url;
                            break;
                        }
                    }
                }
                if (retVidUrl == null) {
                    for (Video video : videos) {
                        if (video.ext.toLowerCase().contains("mp4")
                                && video.type.toLowerCase().contains("low")) {
                            retVidUrl = video.url;
                            break;
                        }
                    }
                }
                if (retVidUrl == null) {
                    for (Video video : videos) {
                        if (video.ext.toLowerCase().contains("3gp")
                                && video.type.toLowerCase().contains("low")) {
                            retVidUrl = video.url;
                            break;
                        }
                    }
                }

                ret = retVidUrl;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private static String getHtml(String url) throws IOException {

        // TODO
        // must keep only argument "v"

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

    private static List<Video> getStreamingUrisFromYouTubePage(String ytUrl) throws IOException {

        String html = getHtml(ytUrl);
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

        List<String> matches = new ArrayList<>();
        {
            Pattern p = Pattern.compile("stream_map\": \"(.*?)?\"");
            // Pattern p = Pattern.compile("/stream_map=(.[^&]*?)\"/");
            Matcher m = p.matcher(html);
            while (m.find()) {
                matches.add(m.group());
            }
        }

        if (matches.size() != 1) {
            return null;
        }

        Map<String, String> foundArray = new HashMap<>();
        String urls[] = matches.get(0).split(",");
        if ((urls != null) && (0 < urls.length)) {
            Pattern p1 = Pattern.compile("itag=([0-9]+?)[&]");
            Pattern p2 = Pattern.compile("sig=(.*?)[&]");
            Pattern p3 = Pattern.compile("url=(.*?)[&]");
            for (String ppUrl : urls) {
                String url = URLDecoder.decode(ppUrl, "UTF-8");

                Matcher m1 = p1.matcher(url);
                String itag = null;
                if (m1.find()) {
                    itag = m1.group(1);
                }

                Matcher m2 = p2.matcher(url);
                String sig = null;
                if (m2.find()) {
                    sig = m2.group(1);
                }

                Matcher m3 = p3.matcher(ppUrl);
                String um = null;
                if (m3.find()) {
                    um = m3.group(1);
                }

                if ((itag != null) && (sig != null) && (um != null)) {
                    foundArray.put(itag, URLDecoder.decode(um, "UTF-8") + "&" + "signature=" + sig);
                }
            }
        }

        if (foundArray.size() == 0) {
            return null;
        }

        List<Video> videos = new ArrayList<>();

        for (Entry<String, Meta> entry : typeMap.entrySet()) {
            String format = entry.getKey();
            Meta meta = entry.getValue();
            if (foundArray.containsKey(format)) {
                Video newVideo = new Video(meta.ext, meta.type, foundArray.get(format));
                videos.add(newVideo);
            }
        }

        return videos;
    }
}

class Meta {

    public String num;

    public String type;

    public String ext;

    Meta(String num, String ext, String type) {
        this.num = num;
        this.ext = ext;
        this.type = type;
    }
}

class Video {

    public String ext;

    public String type;

    public String url;

    Video(String ext, String type, String url) {
        this.ext = ext;
        this.type = type;
        this.url = url;
    }
}
