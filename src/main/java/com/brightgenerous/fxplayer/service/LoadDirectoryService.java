package com.brightgenerous.fxplayer.service;

import java.io.File;
import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.util.Utils;

public class LoadDirectoryService extends Service<List<? extends MediaInfo>> {

    public static interface ICallback {

        void callback(File dir, List<? extends MediaInfo> infos);
    }

    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    {
        File home = Utils.getHomeDirectory();
        if (home != null) {
            directoryChooser.setInitialDirectory(home);
        }
    }

    private final ObservableValue<? extends Window> owner;

    private final MetaChangeListener metaChangeListener;

    private final ICallback callback;

    public LoadDirectoryService(ObservableValue<? extends Window> owner,
            MetaChangeListener metaChangeListener, ICallback callback) {
        this.owner = owner;
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

                File dir = directoryChooser.showDialog(owner.getValue());
                if (isCancelled()) {
                    return null;
                }
                if (dir == null) {
                    return null;
                }
                {
                    File parent = dir.getParentFile();
                    if ((parent != null) && parent.isDirectory()) {
                        directoryChooser.setInitialDirectory(parent);
                    } else {
                        directoryChooser.setInitialDirectory(dir);
                    }
                }
                List<MediaInfo> infos = MediaInfoLoader.fromDirectory(dir, metaChangeListener);

                if (isCancelled()) {
                    return null;
                }

                callback.callback(dir, infos);
                return infos;
            }
        };
    }
}
