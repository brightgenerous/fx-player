package com.brightgenerous.fxplayer.media;

import java.lang.ref.SoftReference;
import java.util.Map;

import javafx.scene.media.Media;

import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.util.IData;
import com.brightgenerous.fxplayer.util.UrlResolver;

public class MediaInfoFactory {

    private final UrlResolver resolver;

    private final Map<String, SoftReference<Media>> mediaCache;

    private final Map<String, SoftReference<IData<String>>> resolveCache;

    public MediaInfoFactory(UrlResolver resolver, Map<String, SoftReference<Media>> mediaCache,
            Map<String, SoftReference<IData<String>>> resolveCache) {
        this.resolver = resolver;
        this.mediaCache = mediaCache;
        this.resolveCache = resolveCache;
    }

    public MediaInfo create(String url, String description, MetaChangeListener metaChangeListener) {
        IData<String> resolve = null;
        if ((url != null) && (resolveCache != null)) {
            SoftReference<IData<String>> sf = resolveCache.get(url);
            if (sf != null) {
                resolve = sf.get();
            }
        }
        if (resolve == null) {
            resolve = resolver.getFileUrl(url);
            if ((url != null) && (resolve != null) && (resolveCache != null)) {
                resolveCache.put(url, new SoftReference<>(resolve));
            }
        }
        return new MediaInfo(new MediaSource(url, resolve, description), metaChangeListener,
                mediaCache);
    }
}
