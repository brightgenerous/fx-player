package com.brightgenerous.fxplayer.service;

import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;

public class LoadUrlService extends Service<List<? extends MediaInfo>> {

    public static interface ICallback {

        void callback(String url, List<? extends MediaInfo> infos);
    }

    private final ObservableValue<String> textProperty;

    private final MetaChangeListener metaChangeListener;

    private final ObservableValue<LoadDirection> loadDirectionProperty;

    private final ICallback callback;

    public LoadUrlService(ObservableValue<String> textProperty,
            MetaChangeListener metaChangeListener,
            ObservableValue<LoadDirection> loadDirectionProperty, ICallback callback) {
        this.textProperty = textProperty;
        this.metaChangeListener = metaChangeListener;
        this.loadDirectionProperty = loadDirectionProperty;
        this.callback = callback;
    }

    @Override
    protected Task<List<? extends MediaInfo>> createTask() {
        return new Task<List<? extends MediaInfo>>() {

            @Override
            protected List<MediaInfo> call() throws Exception {
                if (isCancelled()) {
                    return null;
                }

                String url = textProperty.getValue();
                if (url == null) {
                    return null;
                }
                url = url.trim();
                if (url.isEmpty()) {
                    return null;
                }
                List<MediaInfo> infos = MediaInfoLoader.fromURL(url, metaChangeListener,
                        loadDirectionProperty.getValue());

                if (isCancelled()) {
                    return null;
                }

                callback.callback(url, infos);
                return infos;
            }
        };
    }
}
