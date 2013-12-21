package com.brightgenerous.fxplayer.media;

import java.io.Serializable;

import com.brightgenerous.fxplayer.util.IData;

class MediaSource implements IMediaSource, Serializable {

    private static final long serialVersionUID = 396630729498085565L;

    private final String url;

    private final IData<String> fileUrl;

    private final String description;

    MediaSource(String url, IData<String> fileUrl, String description) {
        this.url = url;
        this.fileUrl = fileUrl;
        this.description = description;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getFileUrl() {
        return fileUrl.get();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void requestResolve(boolean force) {
        fileUrl.request(force);
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
