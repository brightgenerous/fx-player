package com.brightgenerous.fxplayer.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;

public class LoadFilesService extends Service<List<? extends MediaInfo>> {

    public static interface ICallback {

        void callback(List<? extends File> files, List<? extends MediaInfo> infos);
    }

    private final ObservableValue<? extends List<? extends File>> filesProperty;

    private final MetaChangeListener metaChangeListener;

    private final ICallback callback;

    public LoadFilesService(ObservableValue<? extends List<? extends File>> filesProperty,
            MetaChangeListener metaChangeListener, ICallback callback) {
        this.filesProperty = filesProperty;
        this.metaChangeListener = metaChangeListener;
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

                List<? extends File> files = filesProperty.getValue();
                if (files == null) {
                    return null;
                }

                List<MediaInfo> ret = new ArrayList<>();
                for (File file : files) {
                    if (file.isFile()) {
                        ret.addAll(MediaInfoLoader.fromFiles(file, metaChangeListener));
                    } else if (file.isDirectory()) {
                        ret.addAll(MediaInfoLoader.fromDirectory(file, metaChangeListener));
                    }
                }

                if (isCancelled()) {
                    return null;
                }

                callback.callback(files, ret);
                return ret;
            }
        };
    }
}
