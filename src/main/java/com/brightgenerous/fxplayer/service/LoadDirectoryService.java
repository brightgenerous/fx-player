package com.brightgenerous.fxplayer.service;

import java.io.File;
import java.util.List;

import javafx.beans.property.ReadOnlyProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.util.Utils;

public class LoadDirectoryService extends Service<List<MediaInfo>> {

    public static interface ICallback {

        void callback(File dir, List<MediaInfo> infos);
    }

    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    {
        File home = Utils.getHomeDirectory();
        if (home != null) {
            directoryChooser.setInitialDirectory(home);
        }
    }

    private final ReadOnlyProperty<Window> owner;

    private final MetaChangeListener metaChangeListener;

    private final ICallback callback;

    public LoadDirectoryService(ReadOnlyProperty<Window> owner,
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
                File dir = directoryChooser.showDialog(owner.getValue());
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
                callback.callback(dir, infos);
                return infos;
            }
        };
    }
}
