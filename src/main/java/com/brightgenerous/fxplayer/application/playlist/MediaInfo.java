package com.brightgenerous.fxplayer.application.playlist;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.MapChangeListener;
import javafx.concurrent.Service;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.util.Duration;

public class MediaInfo {

    private Media media;

    private final IMediaSource source;

    private final BooleanProperty cursorProperty = new SimpleBooleanProperty();

    private final StringProperty titleProperty = new SimpleStringProperty();

    private final StringProperty artistProperty = new SimpleStringProperty();

    private ObjectProperty<Duration> durationProperty = new SimpleObjectProperty<>();

    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();

    private boolean tryLoaded;

    private final Service<?> loadNextService;

    public MediaInfo(IMediaSource source, Service<?> loadNextService) {
        this.source = source;
        this.loadNextService = loadNextService;
    }

    public boolean loaded() {
        return tryLoaded;
    }

    public boolean load() {
        boolean ret = false;
        try {
            getMedia();
            ret = true;
        } catch (MediaLoadException e) {
        }
        return ret;
    }

    public Media getMedia() throws MediaLoadException {
        if (media == null) {
            try {
                tryLoaded = true;
                media = new Media(source.getUrl());
            } catch (IllegalArgumentException | MediaException e) {
                throw new MediaLoadException(e);
            }
            {
                media.getMetadata().addListener(new MapChangeListener<String, Object>() {

                    @Override
                    public void onChanged(Change<? extends String, ? extends Object> change) {
                        {
                            String title = (String) media.getMetadata().get("title");
                            if (title != null) {
                                titleProperty.setValue(title);
                            }
                        }
                        {
                            String artist = (String) media.getMetadata().get("artist");
                            if (artist != null) {
                                artistProperty.setValue(artist);
                            }
                        }
                        {
                            Duration duration = (Duration) media.getMetadata().get("duration");
                            if (duration != null) {
                                durationProperty.setValue(duration);
                            }
                        }
                        {
                            Image image = (Image) media.getMetadata().get("image");
                            if (image != null) {
                                imageProperty.setValue(image);
                            }
                        }

                        // go to next loading
                        loadNext();

                    }
                });
                titleProperty.setValue((String) media.getMetadata().get("title"));
                artistProperty.setValue((String) media.getMetadata().get("artist"));
                durationProperty.setValue((Duration) media.getMetadata().get("duration"));
                imageProperty.setValue((Image) media.getMetadata().get("image"));
            }
        }
        return media;
    }

    private void loadNext() {
        if ((loadNextService != null) && !loadNextService.isRunning()) {
            loadNextService.restart();
        }
    }

    public Boolean getExist() {
        return Boolean.TRUE;
    }

    public String getDescription() {
        String ret = source.getDescription();
        if (ret == null) {
            ret = source.getUrl();
        }
        return ret;
    }

    public WritableValue<Boolean> cursorProperty() {
        return cursorProperty;
    }

    public ReadOnlyProperty<String> titleProperty() {
        return titleProperty;
    }

    public ReadOnlyProperty<String> artistProperty() {
        return artistProperty;
    }

    public ObjectProperty<Duration> durationProperty() {
        return durationProperty;
    }

    public ObjectProperty<Image> imageProperty() {
        return imageProperty;
    }

    @Override
    public int hashCode() {
        if (source == null) {
            return -1;
        }
        return source.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MediaInfo)) {
            return false;
        }
        MediaInfo arg = (MediaInfo) obj;
        if (source == arg.source) {
            return true;
        }
        if ((source == null) || (arg.source == null)) {
            return false;
        }
        return source.equals(arg.source);
    }
}
