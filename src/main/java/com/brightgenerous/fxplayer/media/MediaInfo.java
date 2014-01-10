package com.brightgenerous.fxplayer.media;

import java.lang.ref.SoftReference;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
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

    public static final Comparator<MediaInfo> SourceUrlComparator = new Comparator<MediaInfo>() {

        @Override
        public int compare(MediaInfo arg0, MediaInfo arg1) {
            if (arg0 == arg1) {
                return 0;
            }
            if (arg0 == null) {
                return 1;
            }
            if (arg1 == null) {
                return -1;
            }
            return IMediaSource.UrlComparator.compare(arg0.source, arg1.source);
        }
    };

    private final ObjectProperty<MediaStatus> mediaStatusProperty = new SimpleObjectProperty<>(
            MediaStatus.MEDIA_YET);

    private final AtomicInteger mediaStatusChanges = new AtomicInteger();

    private final BooleanProperty cursorProperty = new SimpleBooleanProperty(this, "cursor");

    private final StringProperty titleProperty = new SimpleStringProperty(this, "title", "");

    private final StringProperty titleDescProperty = new SimpleStringProperty(this, "titleDesc");

    private final StringProperty descriptionProperty = new SimpleStringProperty(this, "description");

    private final StringProperty artistProperty = new SimpleStringProperty(this, "artist", "");

    private final StringProperty albumProperty = new SimpleStringProperty(this, "album", "");

    private final ObjectProperty<Duration> durationProperty = new SimpleObjectProperty<>(this,
            "duration");

    private final StringProperty durationTextProperty = new SimpleStringProperty(this,
            "durationText", "");

    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>(this, "image");

    private final StringProperty audioCodecProperty = new SimpleStringProperty(this, "audioCodec",
            "");

    private final StringProperty videoCodecProperty = new SimpleStringProperty(this, "videoCodec",
            "");

    private final IntegerProperty widthProperty = new SimpleIntegerProperty(this, "width");

    private final IntegerProperty heightProperty = new SimpleIntegerProperty(this, "height");

    private final DoubleProperty framerateProperty = new SimpleDoubleProperty(this, "framerate");

    private final BooleanProperty visibleTooltipProperty = new SimpleBooleanProperty(this,
            "visibleTooltip");

    private final Property<String> tooltipProperty = new SimpleStringProperty(this, "tooltip");

    private final Property<String> infoProperty = new SimpleStringProperty(this, "info");

    {
        {
            durationProperty.addListener(new ChangeListener<Duration>() {

                @Override
                public void changed(ObservableValue<? extends Duration> observable,
                        Duration oldValue, Duration newValue) {
                    if (newValue != null) {
                        String text = milliSecToTime(newValue.toMillis());
                        if (!durationTextProperty.getValue().equals(text)) {
                            durationTextProperty.setValue(text);
                        }
                    }
                }

                private String milliSecToTime(double millis) {
                    int sec = (int) (millis / 1000);
                    return String.format("%3d:%02d", Integer.valueOf(sec / 60),
                            Integer.valueOf(sec % 60));
                }
            });

            titleProperty.addListener(new ChangeListener<String>() {

                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue,
                        String newValue) {
                    if (newValue != null) {
                        if (!titleDescProperty.getValue().equals(newValue)) {
                            titleDescProperty.setValue(newValue);
                        }
                    }
                }
            });
        }

        BooleanBinding visibleAudioInfo;
        BooleanBinding visibleVideoInfo;
        {
            visibleAudioInfo = titleProperty.isNotEqualTo("").or(artistProperty.isNotEqualTo(""))
                    .or(albumProperty.isNotEqualTo("")).or(imageProperty.isNotNull());
            visibleVideoInfo = videoCodecProperty.isNotEqualTo("")
                    .or(audioCodecProperty.isNotEqualTo("")).or(widthProperty.greaterThan(0))
                    .or(heightProperty.greaterThan(0)).or(framerateProperty.greaterThan(0));
            ObservableBooleanValue visibleOtherwise = mediaStatusProperty.isNotEqualTo(
                    MediaStatus.MEDIA_YET).and(
                    mediaStatusProperty.isNotEqualTo(MediaStatus.MEDIA_ERROR));

            visibleTooltipProperty.bind(visibleAudioInfo.or(visibleVideoInfo).or(visibleOtherwise));
        }
        {
            ObservableStringValue audioTooltip = Bindings.concat("Title : ").concat(titleProperty)
                    .concat("\nArtist : ").concat(artistProperty).concat("\nAlbum : ")
                    .concat(albumProperty).concat("\nDuration : ").concat(durationTextProperty)
                    .concat("\n--\nDescription : ").concat(descriptionProperty);
            ObservableStringValue videoTooltip = Bindings.concat("Video Codec : ")
                    .concat(videoCodecProperty).concat("\nAudio Codec : ")
                    .concat(audioCodecProperty).concat("\nWidth : ").concat(widthProperty)
                    .concat("\nHeight : ").concat(heightProperty).concat("\nFramerate : ")
                    .concat(framerateProperty.asString("%.2f")).concat("\nDuration : ")
                    .concat(durationTextProperty).concat("\n--\nDescription : ")
                    .concat(descriptionProperty);
            ObservableStringValue otherwiseTooltip = Bindings.concat(titleDescProperty)
                    .concat("\nDuration : ").concat(durationTextProperty)
                    .concat("\n--\nDescription : ").concat(descriptionProperty);

            ObservableStringValue tooltip = Bindings
                    .when(visibleVideoInfo)
                    .then(videoTooltip)
                    .otherwise(
                            Bindings.when(visibleAudioInfo).then(audioTooltip)
                                    .otherwise(otherwiseTooltip));
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
                    .concat(framerateProperty.asString("%.2f")).concat(" , Duration : ")
                    .concat(durationTextProperty);
            ObservableStringValue otherwiseInfo = Bindings.concat("Duration : ").concat(
                    durationTextProperty);

            StringBinding info = Bindings
                    .when(visibleVideoInfo)
                    .then(videoInfo)
                    .otherwise(
                            Bindings.when(visibleAudioInfo).then(audioInfo)
                                    .otherwise(otherwiseInfo));
            infoProperty.bind(Bindings.when(visibleTooltipProperty).then(info).otherwise(""));
        }

        {
            // OMAJINAI
            mediaStatusProperty.addListener(new ChangeListener<MediaStatus>() {

                @Override
                public void changed(ObservableValue<? extends MediaStatus> observable,
                        MediaStatus oldValue, MediaStatus newValue) {
                    mediaStatusChanges.incrementAndGet();
                }
            });
        }
    }

    private volatile Media media;

    private volatile MapChangeListener<String, Object> changeListener;

    private volatile boolean tryLoaded;

    private final IMediaSource source;

    private final MetaChangeListener callback;

    private final Map<String, SoftReference<Media>> mediaCache;

    private final Object lock = new Object();

    MediaInfo(IMediaSource source, MetaChangeListener callback,
            Map<String, SoftReference<Media>> mediaCache) {
        this.source = source;
        this.callback = callback;
        titleDescProperty.set(source.getDescription());
        descriptionProperty.set(source.getDescription());
        this.mediaCache = mediaCache;
    }

    public IMediaSource getSource() {
        return source;
    }

    public boolean enablePreLoad() {
        return source.enablePreLoad();
    }

    public boolean loaded() {
        return tryLoaded;
    }

    public boolean load() throws MediaLoadException {
        return getMedia() != null;
    }

    public Media forceResolveIfYet() {
        if (media != null) {
            return media;
        }
        synchronized (lock) {
            if (media != null) {
                return media;
            }
            tryLoaded = false;
            //media = null;
            source.requestResolve(true);
        }
        return null;
    }

    public Media releaseMedia(boolean forceResolve) {
        Media ret;
        synchronized (lock) {
            ret = media;
            tryLoaded = false;
            media = null;
            source.requestResolve(forceResolve);
            if (ret != null) {
                // unbind
                if (changeListener != null) {
                    ret.getMetadata().removeListener(changeListener);
                }
            }
            {
                // reset properties
                titleProperty.setValue("");
                titleDescProperty.setValue(source.getDescription());
                artistProperty.setValue("");
                albumProperty.setValue("");
                durationProperty.setValue(null);
                try {
                    durationTextProperty.setValue("");
                } catch (IllegalStateException e) {
                    // OMAJINAI
                    // exception message => Not on FX application thread; currentThread = XXXXX
                }
                imageProperty.setValue(null);
                audioCodecProperty.setValue("");
                videoCodecProperty.setValue("");
                widthProperty.set(0);
                heightProperty.set(0);
                framerateProperty.set(0);
            }
        }
        return ret;
    }

    public Media getMedia() throws MediaLoadException {
        if (!tryLoaded && (media == null)) {
            synchronized (lock) {
                if (!tryLoaded && (media == null)) {
                    tryLoaded = true;
                    String url = source.getFileUrl();
                    if ((url != null) && (mediaCache != null)) {
                        SoftReference<Media> sf = mediaCache.get(url);
                        if (sf != null) {
                            media = sf.get();
                        }
                    }
                    if (media == null) {
                        media = createMedia(url);
                        if ((url != null) && (media != null) && (mediaCache != null)) {
                            SoftReference<Media> sf = new SoftReference<>(media);
                            mediaCache.put(url, sf);
                        }
                    }
                    if (media != null) {
                        bind(media);
                    }
                }
            }
        }
        return media;
    }

    private Media createMedia(String url) throws MediaLoadException {
        Media ret;
        try {
            if (url == null) {
                throw new MediaLoadException("file url is null.");
            }
            ret = new Media(url);
        } catch (IllegalArgumentException | MediaException e) {

            mediaStatusProperty.setValue(MediaStatus.MEDIA_ERROR);

            throw new MediaLoadException(e);
        }
        return ret;
    }

    private void bind(final Media media) {
        changeListener = new MapChangeListener<String, Object>() {

            @Override
            public void onChanged(Change<? extends String, ? extends Object> change) {
                if (change.wasAdded()) {
                    String key = change.getKey();
                    switch (key) {
                        case "title": {
                            setIfUpdate(change.getMap(), key, titleProperty);
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
                            setIfUpdate(change.getMap(), key, durationProperty);
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
                if (callback != null) {
                    callback.onChanged(MediaInfo.this, media, change);
                }
            }
        };

        media.getMetadata().addListener(changeListener);

        setIfUpdate(media.getMetadata(), "title", titleProperty);
        setIfUpdate(media.getMetadata(), "artist", artistProperty);
        setIfUpdate(media.getMetadata(), "album artist", artistProperty);
        setIfUpdate(media.getMetadata(), "album", albumProperty);
        setIfUpdate(media.getMetadata(), "duration", durationProperty);
        setIfUpdate(media.getMetadata(), "image", imageProperty);
        setIfUpdate(media.getMetadata(), "audio codec", audioCodecProperty);
        setIfUpdate(media.getMetadata(), "video codec", videoCodecProperty);
        setIfUpdate(media.getMetadata(), "width", widthProperty);
        setIfUpdate(media.getMetadata(), "height", heightProperty);
        setIfUpdate(media.getMetadata(), "framerate", framerateProperty);

        try {
            // if this thread is not main thread,
            //   in rare cases, thrown IllegalStateException in the next step.
            mediaStatusProperty.setValue(MediaStatus.MEDIA_SUCCESS);
        } catch (IllegalStateException e) {
            // OMAJINAI
            // exception message => Not on FX application thread; currentThread = XXXXX
            Platform.runLater(new Runnable() {

                int changes = mediaStatusChanges.get();

                @Override
                public void run() {
                    // too late ?
                    if (changes == mediaStatusChanges.get()) {
                        mediaStatusProperty.setValue(MediaStatus.MEDIA_SUCCESS);
                    }
                }
            });
        }
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

    public ReadOnlyProperty<Image> imageProperty() {
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

    private ChangeListener<Duration> durationChangeListener;

    public void replaceDurationChangeListener(ChangeListener<Duration> listener) {
        if (durationChangeListener != null) {
            durationProperty.removeListener(durationChangeListener);
        }
        durationProperty.addListener(listener);
        durationChangeListener = listener;
    }

    private ChangeListener<Image> imageChangeListener;

    public void replaceImageChangeListener(ChangeListener<Image> listener) {
        if (imageChangeListener != null) {
            imageProperty.removeListener(imageChangeListener);
        }
        imageProperty.addListener(listener);
        imageChangeListener = listener;
    }

    private ChangeListener<Boolean> visibleTooltipChangeListener;

    public void replaceVisibleTooltipChangeListener(ChangeListener<Boolean> listener) {
        if (visibleTooltipChangeListener != null) {
            visibleTooltipProperty.removeListener(visibleTooltipChangeListener);
        }
        visibleTooltipProperty.addListener(listener);
        visibleTooltipChangeListener = listener;
    }
}
