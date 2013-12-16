package com.brightgenerous.fxplayer.application.playlist;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.MapChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import com.brightgenerous.fxplayer.application.Utils.Inject;

public class PlayList implements Initializable {

    @Inject
    private Stage owner;

    @Inject
    private ResourceBundle bundle;

    @FXML
    private ImageView imageView;

    @FXML
    private TextField directoryText;

    @FXML
    private Text infoText;

    @FXML
    private TableView<MediaInfo> mediaList;

    @FXML
    private TableColumn<MediaInfo, Boolean> tableColumnIndex;

    @FXML
    private TableColumn<MediaInfo, Boolean> tableColumnCursor;

    @FXML
    private TableColumn<MediaInfo, Duration> tableColumnDuration;

    @FXML
    private Button controlPlay;

    @FXML
    private Slider controlTime;

    @FXML
    private Slider controlVolume;

    private final DirectoryChooser chooser = new DirectoryChooser();

    private File directory;

    private final ReadWriteLock listLock = new ReentrantReadWriteLock();

    private MediaInfo current;

    private MediaPlayer player;

    private long lastCreate = Long.MIN_VALUE;

    private static final double DEFAULT_VOLUME = 0.5d;

    private static final long UPDATE_SEEK_FREQUENCY = 30;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        {
            final Callback<TableView<MediaInfo>, TableRow<MediaInfo>> deleg = mediaList
                    .getRowFactory();
            final EventHandler<MouseEvent> clickListener = new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    if (1 < event.getClickCount()) {
                        Object source = event.getSource();
                        if (source instanceof TableRow) {
                            TableRow<?> row = (TableRow<?>) source;
                            Object obj = row.getItem();
                            if (obj instanceof MediaInfo) {
                                controlPlayer(Control.SPECIFY, (MediaInfo) obj);
                            }
                        }
                    }
                }
            };
            mediaList.setRowFactory(new Callback<TableView<MediaInfo>, TableRow<MediaInfo>>() {

                @Override
                public TableRow<MediaInfo> call(TableView<MediaInfo> param) {
                    TableRow<MediaInfo> ret;
                    if (deleg == null) {
                        ret = new TableRow<>();
                    } else {
                        ret = deleg.call(param);
                    }
                    ret.addEventFilter(MouseEvent.MOUSE_CLICKED, clickListener);
                    return ret;
                }
            });
        }
        tableColumnIndex
                .setCellFactory(new Callback<TableColumn<MediaInfo, Boolean>, TableCell<MediaInfo, Boolean>>() {

                    @Override
                    public TableCell<MediaInfo, Boolean> call(TableColumn<MediaInfo, Boolean> param) {
                        return new TableCell<MediaInfo, Boolean>() {

                            @Override
                            protected void updateItem(Boolean item, boolean empty) {
                                super.updateItem(item, empty);

                                if (!empty) {
                                    setText(String.format("%d",
                                            Integer.valueOf(getTableRow().getIndex() + 1)));
                                    setAlignment(Pos.CENTER_RIGHT);
                                } else {
                                    setText(null);
                                }
                            }
                        };
                    }
                });
        tableColumnCursor
                .setCellFactory(new Callback<TableColumn<MediaInfo, Boolean>, TableCell<MediaInfo, Boolean>>() {

                    @Override
                    public TableCell<MediaInfo, Boolean> call(TableColumn<MediaInfo, Boolean> param) {
                        return new TableCell<MediaInfo, Boolean>() {

                            @Override
                            protected void updateItem(Boolean item, boolean empty) {
                                super.updateItem(item, empty);

                                if ((item != null) && item.booleanValue()) {
                                    setText(bundle.getString("media.crusor"));
                                } else {
                                    setText(null);
                                }
                            }
                        };
                    }
                });
        tableColumnDuration
                .setCellFactory(new Callback<TableColumn<MediaInfo, Duration>, TableCell<MediaInfo, Duration>>() {

                    @Override
                    public TableCell<MediaInfo, Duration> call(
                            TableColumn<MediaInfo, Duration> param) {

                        return new TableCell<MediaInfo, Duration>() {

                            @Override
                            protected void updateItem(Duration item, boolean empty) {
                                super.updateItem(item, empty);

                                if (!empty) {
                                    int sec = (int) item.toSeconds();
                                    setText(String.format("%3d:%02d", Integer.valueOf(sec / 60),
                                            Integer.valueOf(sec % 60)));
                                    setAlignment(Pos.CENTER_RIGHT);
                                } else {
                                    setText(null);
                                }
                            }
                        };
                    }
                });
    }

    @FXML
    protected void controlDirectoryChooser() {
        if (directory != null) {
            chooser.setInitialDirectory(directory);
        }
        File dir = chooser.showDialog(owner);
        if (dir != null) {
            directory = dir;
            if (directoryChooserService.isRunning()) {
                directoryChooserService.cancel();
            }
            directoryChooserService.restart();
        }
    }

    private final Service<Boolean> directoryChooserService = new Service<Boolean>() {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    File dir = directory;
                    if (dir == null) {
                        return Boolean.FALSE;
                    }
                    List<MediaInfo> tmp = new ArrayList<>();
                    for (File file : dir.listFiles()) {
                        if (file.exists() && file.isFile() && file.canRead()) {
                            try {
                                tmp.add(new MediaInfo(new Media(file.toURI().toURL().toString())));
                            } catch (MalformedURLException | MediaException e) {
                            }
                        }
                    }
                    Lock lock = listLock.writeLock();
                    try {
                        lock.lock();
                        directoryText.setText(dir.toString());
                        mediaList.getItems().clear();
                        mediaList.getItems().addAll(tmp);
                    } finally {
                        lock.unlock();
                    }
                    return Boolean.TRUE;
                }
            };
        }
    };

    @FXML
    protected void controlPlay() {
        controlPlayer(Control.PLAY, null);
    }

    @FXML
    protected void controlPrev() {
        controlPlayer(Control.PREV, null);
    }

    @FXML
    protected void controlNext() {
        controlPlayer(Control.NEXT, null);
    }

    private void controlPlayer(Control control, MediaInfo info) {
        player_block: {
            if (control.equals(Control.PLAY)) {
                boolean playing = false;
                boolean paused = false;
                if (player != null) {
                    playing = player.getStatus().equals(Status.PLAYING);
                    paused = player.getStatus().equals(Status.PAUSED);
                }
                if (playing) {
                    player.pause();
                    break player_block;
                } else if (paused) {
                    if (current != null) {
                        List<MediaInfo> items = getItemsSnapshot();
                        int scrollTo = items.indexOf(current);
                        if (scrollTo != -1) {
                            requestScroll(scrollTo);
                        }
                    }
                    player.play();
                    break player_block;
                }
            }

            {
                // OMAJINAI!!!
                long currentTime = System.currentTimeMillis();
                if (currentTime < (lastCreate + 500)) {
                    break player_block;
                }
                lastCreate = currentTime;
            }

            final MediaInfo targetInfo;
            final int scrollTo;
            {
                List<MediaInfo> items = getItemsSnapshot();
                if (items.isEmpty()) {
                    return;
                }
                int index = -1;
                {
                    if (info == null) {
                        info = current;
                    }
                    if (info != null) {
                        index = items.indexOf(info);
                    }
                }
                if (index != -1) {
                    if (control.equals(Control.NEXT)) {
                        index++;
                        if (items.size() <= index) {
                            index = 0;
                        }
                    } else if (control.equals(Control.PREV)) {
                        index--;
                        if (index < 0) {
                            index = items.size() - 1;
                        }
                    }
                } else {
                    index = 0;
                }
                targetInfo = items.get(index);
                scrollTo = index;
            }

            double volume = DEFAULT_VOLUME;
            if (player != null) {
                volume = player.getVolume();
                player.dispose();
            }

            final MediaPlayer mp = new MediaPlayer(targetInfo.getMedia());
            mp.setOnReady(new Runnable() {

                @Override
                public void run() {
                    targetInfo.setDuration(mp.getTotalDuration());
                }
            });
            mp.setOnEndOfMedia(new Runnable() {

                @Override
                public void run() {
                    // no fear, would be called on main thread.
                    controlPlayer(Control.NEXT, null);
                }
            });

            // volume
            {
                mp.setVolume(volume);
                controlVolume.valueProperty().unbind();
                controlVolume.valueProperty().bindBidirectional(mp.volumeProperty());
            }

            {
                // time max
                {
                    final SimpleDoubleProperty totalDuration;
                    {
                        Duration dur = mp.getTotalDuration();
                        if ((dur == null) || dur.equals(Duration.UNKNOWN)) {
                            dur = targetInfo.getDuration();
                        }
                        if ((dur != null) && !dur.equals(Duration.UNKNOWN)) {
                            totalDuration = new SimpleDoubleProperty(dur.toMillis());
                        } else {
                            totalDuration = new SimpleDoubleProperty();
                            targetInfo.durationProperty().addListener(
                                    new WeakChangeListener<>(new ChangeListener<Duration>() {

                                        @Override
                                        public void changed(
                                                ObservableValue<? extends Duration> observable,
                                                Duration oldValue, Duration newValue) {
                                            totalDuration.set(newValue.toMillis());
                                        }
                                    }));
                        }
                    }
                    controlTime.maxProperty().unbind();
                    Double v = totalDuration.getValue();
                    if ((v != null) && (0 < v.doubleValue())) {
                        controlTime.maxProperty().set(v.doubleValue());
                    } else {
                        // OMAJINAI!!!
                        {
                            final double opacity = controlTime.getOpacity();
                            if (controlTime.getUserData() == null) {
                                controlTime.setUserData(Double.valueOf(opacity));
                            }
                            double tmp = opacity - 0.1;
                            if (tmp < 0) {
                                tmp = opacity + 0.1;
                            }
                            controlTime.setOpacity(tmp);
                        }
                        totalDuration.addListener(new WeakInvalidationListener(
                                new InvalidationListener() {

                                    @Override
                                    public void invalidated(Observable arg0) {
                                        Object obj = controlTime.getUserData();
                                        if (obj instanceof Double) {
                                            double opacity = ((Double) obj).doubleValue();
                                            if (controlTime.getOpacity() != opacity) {
                                                controlTime.setOpacity(opacity);
                                            }
                                        }
                                    }
                                }));
                    }
                    controlTime.maxProperty().bind(totalDuration);
                }

                // time current
                {
                    controlTime.valueProperty().addListener(
                            new WeakChangeListener<>(new ChangeListener<Number>() {

                                @Override
                                public void changed(ObservableValue<? extends Number> observable,
                                        Number oldValue, Number newValue) {
                                    if (controlTime.isValueChanging()) {
                                        mp.seek(Duration.millis(newValue.doubleValue()));
                                    }
                                }
                            }));
                    mp.currentTimeProperty().addListener(new InvalidationListener() {

                        private long last;

                        @Override
                        public void invalidated(Observable ov) {
                            final long cv = (long) mp.getCurrentTime().toMillis();
                            if ((last + UPDATE_SEEK_FREQUENCY) < cv) {
                                if (!controlTime.isValueChanging()) {
                                    last = cv;
                                    controlTime.setValue(last);
                                }
                            }
                        }
                    });
                }
            }

            // image
            {
                targetInfo.imageProperty().addListener(
                        new WeakChangeListener<>(new ChangeListener<Image>() {

                            @Override
                            public void changed(ObservableValue<? extends Image> observable,
                                    Image oldValue, Image newValue) {
                                imageView.setImage(newValue);
                                requestScroll(scrollTo);
                            }
                        }));
                imageView.setImage(targetInfo.imageProperty().get());
                requestScroll(scrollTo);
            }

            // cursor
            {
                if (current != null) {
                    current.setCursor(false);
                }
                targetInfo.setCursor(true);
            }

            mp.play();

            current = targetInfo;
            player = mp;
        }
    }

    private List<MediaInfo> getItemsSnapshot() {
        List<MediaInfo> ret;
        Lock lLock = listLock.readLock();
        try {
            lLock.lock();
            ret = new ArrayList<>(mediaList.getItems());
        } finally {
            lLock.unlock();
        }
        return ret;
    }

    private void requestScroll(final int row) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                mediaList.scrollTo(row);
            }
        });
    }

    private enum Control {
        PLAY, PREV, NEXT, SPECIFY;
    }

    public static class MediaInfo {

        private final Media media;

        private final String url;

        private Duration duration;

        private final SimpleBooleanProperty cursorProperty = new SimpleBooleanProperty();

        private ObjectProperty<Duration> durationProperty;

        private ObjectProperty<Image> imageProperty;

        public MediaInfo(Media media) {
            this.media = media;
            url = media.getSource();
        }

        public Media getMedia() {
            return media;
        }

        public SimpleBooleanProperty cursorProperty() {
            return cursorProperty;
        }

        public void setCursor(boolean cursor) {
            cursorProperty.set(cursor);
        }

        public Boolean getExist() {
            return Boolean.TRUE;
        }

        private String getTitle() {
            return (String) media.getMetadata().get("title");
        }

        public StringProperty titleProperty() {
            String title = getTitle();
            final StringProperty ret = new SimpleStringProperty(title);
            if (title == null) {
                media.getMetadata().addListener(new MapChangeListener<String, Object>() {

                    @Override
                    public void onChanged(Change<? extends String, ? extends Object> change) {
                        String title = getTitle();
                        if (title != null) {
                            ret.setValue(title);
                        }
                    }
                });
            }
            return ret;
        }

        private String getArtist() {
            return (String) media.getMetadata().get("artist");
        }

        public StringProperty artistProperty() {
            String atrist = getArtist();
            final StringProperty ret = new SimpleStringProperty(atrist);
            if (atrist == null) {
                media.getMetadata().addListener(new MapChangeListener<String, Object>() {

                    @Override
                    public void onChanged(Change<? extends String, ? extends Object> change) {
                        String artist = getArtist();
                        if (artist != null) {
                            ret.setValue(artist);
                        }
                    }
                });
            }
            return ret;
        }

        private Duration getDuration() {
            if (duration != null) {
                return duration;
            }
            Duration dur = (Duration) media.getMetadata().get("duration");
            if (dur != null) {
                duration = dur;
            }
            return duration;
        }

        public void setDuration(Duration duration) {
            if (duration != null) {
                if (durationProperty != null) {
                    durationProperty.setValue(duration);
                }
                this.duration = duration;
            }
        }

        public ObjectProperty<Duration> durationProperty() {
            if (durationProperty == null) {
                Duration duration = getDuration();
                durationProperty = new SimpleObjectProperty<>(duration);
                if (duration == null) {
                    media.getMetadata().addListener(new MapChangeListener<String, Object>() {

                        @Override
                        public void onChanged(Change<? extends String, ? extends Object> change) {
                            Duration duration = getDuration();
                            if (duration != null) {
                                durationProperty.setValue(duration);
                            }
                        }
                    });
                }
            }
            return durationProperty;
        }

        private Image getImage() {
            return (Image) media.getMetadata().get("image");
        }

        public ObjectProperty<Image> imageProperty() {
            Image image = getImage();
            imageProperty = new SimpleObjectProperty<>(image);
            if (image == null) {
                media.getMetadata().addListener(new MapChangeListener<String, Object>() {

                    @Override
                    public void onChanged(Change<? extends String, ? extends Object> change) {
                        Image image = getImage();
                        if (image != null) {
                            imageProperty.setValue(image);
                        }
                    }
                });
            }
            return imageProperty;
        }

        @Override
        public int hashCode() {
            if (url == null) {
                return -1;
            }
            return url.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MediaInfo)) {
                return false;
            }
            MediaInfo arg = (MediaInfo) obj;
            if (url == arg.url) {
                return true;
            }
            if ((url == null) || (arg.url == null)) {
                return false;
            }
            return url.equals(arg.url);
        }
    }
}
