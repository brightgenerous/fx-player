package com.brightgenerous.fxplayer.service;

import java.io.File;
import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.util.Utils;

public class LoadListFileService extends Service<List<MediaInfo>> {

    public static interface ICallback {

        void callback(File file, List<MediaInfo> infos);
    }

    private final FileChooser fileChooser = new FileChooser();
    {
        File home = Utils.getHomeDirectory();
        if (home != null) {
            fileChooser.setInitialDirectory(home);
        }
    }

    private final ObservableValue<? extends Window> owner;

    private final MetaChangeListener metaChangeListener;

    private final ICallback callback;

    public LoadListFileService(ObservableValue<? extends Window> owner,
            MetaChangeListener metaChangeListener, ICallback callback) {
        this.owner = owner;
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

                File file = fileChooser.showOpenDialog(owner.getValue());
                if (isCancelled()) {
                    return null;
                }
                if (file == null) {
                    return null;
                }
                {
                    File parent = file.getParentFile();
                    if ((parent != null) && parent.isDirectory()) {
                        fileChooser.setInitialDirectory(parent);
                    }
                }
                List<MediaInfo> infos = MediaInfoLoader.fromListFile(file, metaChangeListener);

                if (isCancelled()) {
                    return null;
                }

                callback.callback(file, infos);
                return infos;
            }
        };
    }
}
