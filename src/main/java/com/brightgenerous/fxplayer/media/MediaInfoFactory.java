package com.brightgenerous.fxplayer.media;

import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.util.UrlResolver;

public class MediaInfoFactory {

    private final UrlResolver resolver;

    public MediaInfoFactory(UrlResolver resolver) {
        this.resolver = resolver;
    }

    public MediaInfo create(String url, String description, MetaChangeListener metaChangeListener) {
        return new MediaInfo(new MediaSource(url, resolver.getFileUrl(url), description),
                metaChangeListener);
    }
}
