package com.brightgenerous.fxplayer.url;

import com.brightgenerous.fxplayer.util.IVideoInfo;

class VideoInfo implements IVideoInfo {

    private final String url;

    private String title;

    VideoInfo(String url, String title) {
        this.url = url;
        this.title = title;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }
}
