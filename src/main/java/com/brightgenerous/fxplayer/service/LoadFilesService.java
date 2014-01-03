package com.brightgenerous.fxplayer.service;

import java.io.File;
import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;

public class LoadFilesService extends Service<List<MediaInfo>> {

    public static interface ICallback {

        void callback(List<File> files, List<MediaInfo> infos);
    }

    private final ObservableValue<List<File>> filesProperty;

    private final MetaChangeListener metaChangeListener;

    private final ICallback callback;

    public LoadFilesService(ObservableValue<List<File>> filesProperty,
            MetaChangeListener metaChangeListener, ICallback callback) {
        this.filesProperty = filesProperty;
        this.metaChangeListener = metaChangeListener;
        this.callback = callback;
    }

    @Override
    protected Task<List<MediaInfo>> createTask() {
        return new Task<List<MediaInfo>>() {

            @Override
            protected List<MediaInfo> call() throws Exception {
                if (isCancelled()) {
                    return null;
                }

                List<File> files = filesProperty.getValue();
                if (files == null) {
                    return null;
                }

                List<MediaInfo> infos = MediaInfoLoader.fromFiles(files, metaChangeListener);

                if (isCancelled()) {
                    return null;
                }

                callback.callback(files, infos);
                return infos;
            }
        };
    }
}
