package com.brightgenerous.fxplayer.url;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.brightgenerous.fxplayer.util.IVideoInfo;

public class UrlDispathcer {

    public static enum Service {
        YOUTUBE, NICONICO, MYSERVICE;
    }

    private UrlDispathcer() {
    }

    public static String createQueryUrl(String word, Integer page, Service service)
            throws UnsupportedEncodingException {
        String ret = null;
        switch (service) {
            case YOUTUBE:
                ret = YoutubeUtils.getQueryUrl(word, page);
                break;
            case NICONICO:
                ret = NiconicoUtils.getQueryUrl(word, page);
                break;
            case MYSERVICE:
                ret = MyServiceUtils.getQueryUrl(word);
                break;
        }
        return ret;
    }

    public static String getQueryPageUrl(String url, int inc) {
        String ret = null;
        {
            ret = YoutubeUtils.getQueryPageUrl(url, inc);
        }
        if (ret == null) {
            ret = NiconicoUtils.getQueryPageUrl(url, inc);
        }
        return ret;
    }

    public static List<IVideoInfo> fromUrl(String url, String text, String serverPath,
            String dirPath) {
        List<IVideoInfo> ret = null;
        if (YoutubeUtils.isPlaylistUrl(url)) {
            ret = YoutubeUtils.parsePlaylist(text);
        } else if (YoutubeUtils.isVideoUrl(url)) {
            String title = YoutubeUtils.extractTitle(text);
            ret = new ArrayList<>();
            ret.add(new VideoInfo(url, title));
        } else if (NiconicoUtils.isPlaylistUrl(url)) {
            ret = NiconicoUtils.parsePlaylist(url, text);
        } else if (NiconicoUtils.isVideoUrl(url)) {
            String title = NiconicoUtils.extractTitle(text);
            ret = new ArrayList<>();
            ret.add(new VideoInfo(url, title));
        } else if (XvideosUtils.isVideoUrl(url)) {
            String title = XvideosUtils.extractTitle(text);
            ret = new ArrayList<>();
            ret.add(new VideoInfo(url, title));
        } else {
            ret = MyServiceUtils.fromServer(text, serverPath, dirPath);
        }
        return ret;
    }

    public static List<IVideoInfo> fromFile(File file, File parent) {
        return MyServiceUtils.fromFile(file, parent);
    }

    public static void setNiconicoMail(String mail) {
        NiconicoUtils.setMail(mail);
    }

    public static void setNiconicoPass(String pass) {
        NiconicoUtils.setPass(pass);
    }
}
