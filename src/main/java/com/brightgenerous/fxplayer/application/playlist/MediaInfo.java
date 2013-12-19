package com.brightgenerous.fxplayer.application.playlist;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.util.Duration;

public class MediaInfo {

    public static interface MetaChangeListener {

        void onChanged(Media media, Change<? extends String, ? extends Object> change);
    }

    private Media media;

    private final IMediaSource source;

    private final BooleanProperty cursorProperty = new SimpleBooleanProperty();

    private final StringProperty titleProperty = new SimpleStringProperty("");

    private final StringProperty titleDescProperty = new SimpleStringProperty();

    private final StringProperty artistProperty = new SimpleStringProperty("");

    private final StringProperty albumProperty = new SimpleStringProperty("");

    private final ObjectProperty<Duration> durationProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();

    private final BooleanProperty visibleProperty = new SimpleBooleanProperty();
    {
        visibleProperty.bind(titleProperty.isNotEqualTo("").or(artistProperty.isNotEqualTo(""))
                .or(albumProperty.isNotEqualTo("")).or(imageProperty.isNotNull()));
    }

    private final StringProperty tooltipProperty = new SimpleStringProperty();
    {
        tooltipProperty.bind(Bindings.concat("title : ").concat(titleProperty)
                .concat("\nartist : ").concat(artistProperty).concat("\nalbum : ")
                .concat(albumProperty));
    }

    private boolean tryLoaded;

    private final MetaChangeListener metaChangeListener;

    public MediaInfo(IMediaSource source, MetaChangeListener metaChangeListener) {
        this.source = source;
        this.metaChangeListener = metaChangeListener;
        titleDescProperty.set(source.getDescription());
    }

    public IMediaSource getSource() {
        return source;
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
            media.getMetadata().addListener(new MapChangeListener<String, Object>() {

                @Override
                public void onChanged(Change<? extends String, ? extends Object> change) {
                    if (change.wasAdded()) {
                        String key = change.getKey();
                        switch (key) {
                            case "title": {
                                String title = (String) change.getMap().get(key);
                                if (title != null) {
                                    titleProperty.setValue(title);
                                    title = title.trim();
                                    if (!title.isEmpty()) {
                                        titleDescProperty.setValue(title);
                                    }
                                }
                                break;
                            }
                            case "artist": {
                                String artist = (String) change.getMap().get(key);
                                if (artist != null) {
                                    artistProperty.setValue(artist);
                                }
                                break;
                            }
                            case "album": {
                                String album = (String) change.getMap().get(key);
                                if (album != null) {
                                    albumProperty.setValue(album);
                                }
                                break;
                            }
                            case "duration": {
                                Duration duration = (Duration) change.getMap().get(key);
                                if (duration != null) {
                                    durationProperty.setValue(duration);
                                }
                                break;
                            }
                            case "image": {
                                Image image = (Image) change.getMap().get(key);
                                if (image != null) {
                                    imageProperty.setValue(image);
                                }
                                break;
                            }
                        }
                    }
                    if (metaChangeListener != null) {
                        metaChangeListener.onChanged(media, change);
                    }
                }
            });

            {
                {
                    String title = (String) media.getMetadata().get("title");
                    if (title != null) {
                        titleProperty.setValue(title);
                        title = title.trim();
                        if (!title.isEmpty()) {
                            titleDescProperty.setValue(title);
                        }
                    }
                }
                {
                    String artist = (String) media.getMetadata().get("artist");
                    if (artist != null) {
                        artistProperty.setValue(artist);
                    }
                }
                {
                    String album = (String) media.getMetadata().get("album");
                    if (album != null) {
                        albumProperty.setValue(album);
                    }
                }
                durationProperty.setValue((Duration) media.getMetadata().get("duration"));
                imageProperty.setValue((Image) media.getMetadata().get("image"));
            }
        }
        return media;
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

    public ReadOnlyProperty<String> titleDescProperty() {
        return titleDescProperty;
    }

    public ReadOnlyProperty<String> artistProperty() {
        return artistProperty;
    }

    public ReadOnlyProperty<String> albumProperty() {
        return albumProperty;
    }

    public ObjectProperty<Duration> durationProperty() {
        return durationProperty;
    }

    public ObjectProperty<Image> imageProperty() {
        return imageProperty;
    }

    public ReadOnlyProperty<Boolean> visibleProperty() {
        return visibleProperty;
    }

    public ReadOnlyProperty<String> tooltipProperty() {
        return tooltipProperty;
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
