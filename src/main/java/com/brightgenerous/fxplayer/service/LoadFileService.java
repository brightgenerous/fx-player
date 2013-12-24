package com.brightgenerous.fxplayer.service;

import java.io.File;
import java.util.List;

import javafx.beans.property.ReadOnlyProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.util.Utils;

public class LoadFileService extends Service<List<MediaInfo>> {

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

    private final ReadOnlyProperty<Window> owner;

    private final MetaChangeListener metaChangeListener;

    private final ICallback callback;

    public LoadFileService(ReadOnlyProperty<Window> owner, MetaChangeListener metaChangeListener,
            ICallback callback) {
        this.owner = owner;
        this.metaChangeListener = metaChangeListener;
        this.callback = callback;
    }

    @Override
    protected Task<List<MediaInfo>> createTask() {
        return new Task<List<MediaInfo>>() {

            @Override
            protected List<MediaInfo> call() throws Exception {
                File file = fileChooser.showOpenDialog(owner.getValue());
                if (file == null) {
                    return null;
                }
                {
                    File parent = file.getParentFile();
                    if ((parent != null) && parent.isDirectory()) {
                        fileChooser.setInitialDirectory(parent);
                    }
                }
                List<MediaInfo> infos = MediaInfoLoader.fromFile(file, metaChangeListener);
                callback.callback(file, infos);
                return infos;
            }
        };
    }
}
