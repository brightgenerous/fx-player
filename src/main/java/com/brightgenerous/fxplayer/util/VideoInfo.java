package com.brightgenerous.fxplayer.util;

public class VideoInfo {

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
