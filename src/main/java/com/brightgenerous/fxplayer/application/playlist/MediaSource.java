package com.brightgenerous.fxplayer.application.playlist;

import java.io.Serializable;

public class MediaSource implements IMediaSource, Serializable {

    private static final long serialVersionUID = 396630729498085565L;

    private final String url;

    private final String description;

    public MediaSource(String url) {
        this(url, url);
    }

    public MediaSource(String url, String description) {
        this.url = url;
        this.description = description;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        if (url == null) {
            return -1;
        }
        return url.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MediaSource)) {
            return false;
        }
        MediaSource arg = (MediaSource) obj;
        if (url == arg.url) {
            return true;
        }
        if ((url == null) || (arg.url == null)) {
            return false;
        }
        return url.equals(arg.url);
    }
}
