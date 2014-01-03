package com.brightgenerous.fxplayer.service;

import java.io.File;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.brightgenerous.fxplayer.util.ImageSaveUtils;
import com.brightgenerous.fxplayer.util.ImageSaveUtils.Type;
import com.brightgenerous.fxplayer.util.Utils;

public class SaveImageService extends Service<File> {

    public static class ImageInfo {

        private final Image image;

        private final String name;

        public ImageInfo(Image image, String name) {
            this.image = image;
            this.name = name;
        }

        public Image getImage() {
            return image;
        }

        public String getName() {
            return name;
        }
    }

    public static interface ICallback {

        void callback(File in, File out, ImageInfo info);
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

    private final ObservableValue<? extends ImageInfo> imageInfoProperty;

    private final ObservableValue<? extends Window> owner;

    private final ICallback callback;

    public SaveImageService(ObservableValue<? extends ImageInfo> imageInfoProperty,
            ObservableValue<? extends Window> owner, ICallback callback) {
        this.imageInfoProperty = imageInfoProperty;
        this.owner = owner;
        this.callback = callback;
    }

    @Override
    protected Task<File> createTask() {
        return new Task<File>() {

            @Override
            protected File call() throws Exception {
                if (isCancelled()) {
                    return null;
                }

                ImageInfo info = imageInfoProperty.getValue();
                if (info == null) {
                    return null;
                }

                Image image = info.getImage();
                if (image == null) {
                    return null;
                }

                File file;
                {
                    String name = info.getName();
                    if ((name == null) || name.isEmpty()) {
                        saveChooser.setInitialFileName("");
                    } else {
                        Type type = ImageSaveUtils.suggestType();
                        saveChooser.setInitialFileName(ImageSaveUtils.escapeFileName(name
                                + type.getExtension()));
                    }
                    file = saveChooser.showSaveDialog(owner.getValue());
                }

                if (isCancelled()) {
                    return null;
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
