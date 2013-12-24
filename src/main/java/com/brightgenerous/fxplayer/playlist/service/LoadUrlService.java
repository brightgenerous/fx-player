package com.brightgenerous.fxplayer.playlist.service;

import java.util.List;

import javafx.beans.property.ReadOnlyProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;

public class LoadUrlService extends Service<List<MediaInfo>> {

    public static interface ICallback {

        void callback(String url, List<MediaInfo> infos);
    }

    private final ReadOnlyProperty<String> textProperty;

    private final MetaChangeListener metaChangeListener;

    private final ICallback callback;

    public LoadUrlService(ReadOnlyProperty<String> textProperty,
            MetaChangeListener metaChangeListener, ICallback callback) {
        this.textProperty = textProperty;
        this.metaChangeListener = metaChangeListener;
        this.callback = callback;
    }

    @Override
    protected Task<List<MediaInfo>> createTask() {
        return new Task<List<MediaInfo>>() {

            @Override
            protected List<MediaInfo> call() throws Exception {
                String url = textProperty.getValue();
                if (url == null) {
                    return null;
                }
                url = url.trim();
                if (url.isEmpty()) {
                    return null;
                }
                List<MediaInfo> infos = MediaInfoLoader.fromURL(url, metaChangeListener);
                callback.callback(url, infos);
                return infos;
            }
        };
    }
}
