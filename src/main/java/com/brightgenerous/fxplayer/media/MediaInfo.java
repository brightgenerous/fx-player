package com.brightgenerous.fxplayer.media;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.WritableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.util.Duration;

public class MediaInfo {

    public static interface MetaChangeListener {

        void onChanged(MediaInfo info, Media media,
                Change<? extends String, ? extends Object> change);
    }

    private volatile Media media;

    private final IMediaSource source;

    private final ObjectProperty<MediaStatus> mediaStatusProperty = new SimpleObjectProperty<>(
            MediaStatus.MEDIA_YET);

    private final BooleanProperty cursorProperty = new SimpleBooleanProperty();

    private final StringProperty titleProperty = new SimpleStringProperty("");

    private final StringProperty titleDescProperty = new SimpleStringProperty();

    private final StringProperty artistProperty = new SimpleStringProperty("");

    private final StringProperty albumProperty = new SimpleStringProperty("");

    private final ObjectProperty<Duration> durationProperty = new SimpleObjectProperty<>();

    private final WritableValue<String> durationTextProperty = new SimpleStringProperty("");

    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();

    private final StringProperty audioCodecProperty = new SimpleStringProperty("");

    private final StringProperty videoCodecProperty = new SimpleStringProperty("");

    private final IntegerProperty widthProperty = new SimpleIntegerProperty();

    private final IntegerProperty heightProperty = new SimpleIntegerProperty();

    private final DoubleProperty framerateProperty = new SimpleDoubleProperty();

    private final BooleanProperty visibleTooltipProperty = new SimpleBooleanProperty();

    private final Property<String> tooltipProperty = new SimpleStringProperty();

    private final Property<String> infoProperty = new SimpleStringProperty();

    {
        ObservableBooleanValue visibleVideoInfo;
        {
            BooleanExpression visibleAudioInfo = titleProperty.isNotEqualTo("")
                    .or(artistProperty.isNotEqualTo("")).or(albumProperty.isNotEqualTo(""))
                    .or(imageProperty.isNotNull());
            visibleVideoInfo = videoCodecProperty.isNotEqualTo("")
                    .or(audioCodecProperty.isNotEqualTo("")).or(widthProperty.greaterThan(0))
                    .or(heightProperty.greaterThan(0)).or(framerateProperty.greaterThan(0));

            visibleTooltipProperty.bind(visibleAudioInfo.or(visibleVideoInfo));
        }
        {
            ObservableStringValue audioTooltip = Bindings.concat("Title : ").concat(titleProperty)
                    .concat("\nArtist : ").concat(artistProperty).concat("\nAlbum : ")
                    .concat(albumProperty).concat("\nDuration : ").concat(durationTextProperty);
            ObservableStringValue videoTooltip = Bindings.concat("Video Codec : ")
                    .concat(videoCodecProperty).concat("\nAudio Codec : ")
                    .concat(audioCodecProperty).concat("\nWidth : ").concat(widthProperty)
                    .concat("\nHeight : ").concat(heightProperty).concat("\nFramerate : ")
                    .concat(framerateProperty).concat("\nDuration : ").concat(durationTextProperty);

            ObservableStringValue tooltip = Bindings.when(visibleVideoInfo).then(videoTooltip)
                    .otherwise(audioTooltip);
            tooltipProperty.bind(Bindings.when(visibleTooltipProperty).then(tooltip).otherwise(""));
        }
        {
            ObservableStringValue audioInfo = Bindings.concat("Title : ").concat(titleProperty)
                    .concat(" , Artist : ").concat(artistProperty).concat(" , Album : ")
                    .concat(albumProperty).concat(" , Duration : ").concat(durationTextProperty);
            ObservableStringValue videoInfo = Bindings.concat("Video Codec : ")
                    .concat(videoCodecProperty).concat(" , Audio Codec : ")
                    .concat(audioCodecProperty).concat(" , Width : ").concat(widthProperty)
                    .concat(" , Height : ").concat(heightProperty).concat(" , Framerate : ")
                    .concat(framerateProperty).concat(" , Duration : ")
                    .concat(durationTextProperty);

            StringBinding info = Bindings.when(visibleVideoInfo).then(videoInfo)
                    .otherwise(audioInfo);
            infoProperty.bind(Bindings.when(visibleTooltipProperty).then(info).otherwise(""));
        }
    }

    private volatile boolean tryLoaded;

    private final MetaChangeListener metaChangeListener;

    MediaInfo(IMediaSource source, MetaChangeListener metaChangeListener) {
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

    public boolean load() throws MediaLoadException {
        return getMedia() != null;
    }

    public void resetIfYet(boolean forceResolve) {
        if (media != null) {
            return;
        }
        synchronized (this) {
            if (media != null) {
                return;
            }
            tryLoaded = false;
            media = null;
            source.requestResolve(forceResolve);
        }
    }

    public Media getMedia() throws MediaLoadException {
        if (!tryLoaded && (media == null)) {
            synchronized (this) {
                if (!tryLoaded && (media == null)) {
                    media = createMedia();
                }
            }
        }
        return media;
    }

    private Media createMedia() throws MediaLoadException {
        final Media ret;
        {
            try {
                tryLoaded = true;
                String url = source.getFileUrl();
                if (url == null) {
                    throw new MediaLoadException("file url is null.");
                }
                ret = new Media(url);
            } catch (IllegalArgumentException | MediaException e) {

                mediaStatusProperty.setValue(MediaStatus.MEDIA_ERROR);

                throw new MediaLoadException(e);
            }
            ret.getMetadata().addListener(new MapChangeListener<String, Object>() {

                @Override
                public void onChanged(Change<? extends String, ? extends Object> change) {
                    if (change.wasAdded()) {
                        String key = change.getKey();
                        switch (key) {
                            case "title": {
                                setIfUpdate(change.getMap(), key, titleProperty, titleDescProperty);
                                break;
                            }
                            case "artist":
                            case "album artist": {
                                setIfUpdate(change.getMap(), key, artistProperty);
                                break;
                            }
                            case "album": {
                                setIfUpdate(change.getMap(), key, albumProperty);
                                break;
                            }
                            case "duration": {
                                Duration newValue = setIfUpdate(change.getMap(), key,
                                        durationProperty);
                                if (newValue != null) {
                                    durationTextProperty.setValue(milliSecToTime(newValue
                                            .toMillis()));
                                }
                                break;
                            }
                            case "image": {
                                setIfUpdate(change.getMap(), key, imageProperty);
                                break;
                            }
                            case "audio codec": {
                                setIfUpdate(change.getMap(), key, audioCodecProperty);
                                break;
                            }
                            case "video codec": {
                                setIfUpdate(change.getMap(), key, videoCodecProperty);
                                break;
                            }
                            case "width": {
                                setIfUpdate(change.getMap(), key, widthProperty);
                                break;
                            }
                            case "height": {
                                setIfUpdate(change.getMap(), key, heightProperty);
                                break;
                            }
                            case "framerate": {
                                setIfUpdate(change.getMap(), key, framerateProperty);
                                break;
                            }
                        }
                    }
                    if (metaChangeListener != null) {
                        metaChangeListener.onChanged(MediaInfo.this, ret, change);
                    }
                }

                private String milliSecToTime(double millis) {
                    int sec = (int) (millis / 1000);
                    return String.format("%3d:%02d", Integer.valueOf(sec / 60),
                            Integer.valueOf(sec % 60));
                }
            });

            setIfUpdate(ret.getMetadata(), "title", titleProperty, titleDescProperty);
            setIfUpdate(ret.getMetadata(), "artist", artistProperty);
            setIfUpdate(ret.getMetadata(), "album artist", artistProperty);
            setIfUpdate(ret.getMetadata(), "album", albumProperty);
            setIfUpdate(ret.getMetadata(), "duration", durationProperty);
            setIfUpdate(ret.getMetadata(), "image", imageProperty);
            setIfUpdate(ret.getMetadata(), "audio codec", audioCodecProperty);
            setIfUpdate(ret.getMetadata(), "video codec", videoCodecProperty);
            setIfUpdate(ret.getMetadata(), "width", widthProperty);
            setIfUpdate(ret.getMetadata(), "height", heightProperty);
            setIfUpdate(ret.getMetadata(), "framerate", framerateProperty);

            mediaStatusProperty.setValue(MediaStatus.MEDIA_SUCCESS);
        }
        return ret;
    }

    private static <T> T setIfUpdate(ObservableMap<? extends String, ? extends Object> map,
            String key, WritableValue<T>... props) {
        T newValue = (T) map.get(key);
        if (newValue != null) {
            boolean stringEmpty = false;
            if (newValue instanceof String) {
                String newStr = (String) newValue;
                stringEmpty = newStr.isEmpty();
            }
            for (WritableValue<T> prop : props) {
                if (stringEmpty) {
                    if (prop.getValue() == null) {
                        prop.setValue(newValue);
                    }
                } else {
                    prop.setValue(newValue);
                }
            }
        }
        return newValue;
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

    public BooleanProperty cursorProperty() {
        return cursorProperty;
    }

    public ObjectProperty<MediaStatus> mediaStatusProperty() {
        return mediaStatusProperty;
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

    public ReadOnlyProperty<String> infoProperty() {
        return infoProperty;
    }

    public Property<Image> imageProperty() {
        return imageProperty;
    }

    public int getWidth() {
        return widthProperty.get();
    }

    public int getHeight() {
        return heightProperty.get();
    }

    public ReadOnlyProperty<Boolean> visibleTooltipProperty() {
        return visibleTooltipProperty;
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
