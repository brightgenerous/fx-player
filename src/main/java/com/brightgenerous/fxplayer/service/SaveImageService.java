package com.brightgenerous.fxplayer.service;

import java.io.File;

import javafx.beans.property.ReadOnlyProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.util.ImageSaveUtils;
import com.brightgenerous.fxplayer.util.ImageSaveUtils.Type;
import com.brightgenerous.fxplayer.util.Utils;

public class SaveImageService extends Service<File> {

    public static interface ICallback {

        void callback(File in, File out, MediaInfo info);
    }

    private final FileChooser saveChooser = new FileChooser();
    {
        File home = Utils.getHomeDirectory();
        if (home != null) {
            saveChooser.setInitialDirectory(home);
        }
        saveChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(ImageSaveUtils.getFileDescription(), ImageSaveUtils
                        .getExtensions()));
    }

    private final ReadOnlyProperty<MediaInfo> infoProperty;

    private final ReadOnlyProperty<Window> owner;

    private final ICallback callback;

    public SaveImageService(ReadOnlyProperty<MediaInfo> infoProperty,
            ReadOnlyProperty<Window> owner, ICallback callback) {
        this.infoProperty = infoProperty;
        this.owner = owner;
        this.callback = callback;
    }

    @Override
    protected Task<File> createTask() {
        return new Task<File>() {

            @Override
            protected File call() throws Exception {
                MediaInfo info = infoProperty.getValue();
                if (info == null) {
                    return null;
                }

                Image image = info.imageProperty().getValue();
                if (image == null) {
                    return null;
                }

                File file;
                {
                    String title = info.titleProperty().getValue();
                    if ((title == null) || title.isEmpty()) {
                        saveChooser.setInitialFileName("");
                    } else {
                        Type type = ImageSaveUtils.suggestType();
                        saveChooser.setInitialFileName(ImageSaveUtils.escapeFileName(title
                                + type.getExtension()));
                    }
                    file = saveChooser.showSaveDialog(owner.getValue());
                }
                if (file == null) {
                    return null;
                }
                {
                    File parent = file.getParentFile();
                    if ((parent != null) && parent.isDirectory()) {
                        saveChooser.setInitialDirectory(parent);
                    }
                }

                File out = ImageSaveUtils.save(file, image);
                callback.callback(file, out, info);
                return out;
            }
        };
    }

}
