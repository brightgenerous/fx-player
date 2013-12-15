package com.brightgenerous.fxplayer.application.scene.playlist;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
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

public class PlayListScene implements Initializable {

    @Inject
    private Stage owner;

    @Inject
    private ResourceBundle bundle;

    @FXML
    private ImageView image;

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

    // lock priority high
    private final ReadWriteLock listLock = new ReentrantReadWriteLock();

    // lock priority low
    private final ReadWriteLock playerLock = new ReentrantReadWriteLock();

    private volatile MediaInfo current;

    private volatile MediaPlayer player;

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
                public TableRow<MediaInfo> call(TableView<MediaInfo> arg0) {
                    TableRow<MediaInfo> ret;
                    if (deleg == null) {
                        ret = new TableRow<>();
                    } else {
                        ret = deleg.call(arg0);
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
                    public TableCell<MediaInfo, Boolean> call(TableColumn<MediaInfo, Boolean> arg0) {
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

    private volatile File directory;

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
                                String url = file.toURI().toURL().toString();
                                Media media = new Media(url);
                                tmp.add(new MediaInfo(media));
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
        final MediaInfo nextInfo;
        int scrollTo;
        {
            Lock lLock = listLock.readLock();
            try {
                lLock.lock();

                List<MediaInfo> items = new ArrayList<>(mediaList.getItems());
                if (items.isEmpty()) {
                    return;
                }
                int index = -1;
                if (control.equals(Control.SPECIFY)) {
                    index = items.indexOf(info);
                } else {
                    if (current != null) {
                        index = items.indexOf(current);
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
                    }
                }
                if (index < 0) {
                    index = 0;
                }
                nextInfo = items.get(index);
                scrollTo = index;
            } finally {
                lLock.unlock();
            }
        }
        {
            Lock pLock = playerLock.writeLock();
            try {
                pLock.lock();

                boolean playing = false;
                boolean paused = false;
                if (player != null) {
                    playing = player.getStatus().equals(Status.PLAYING);
                    paused = player.getStatus().equals(Status.PAUSED);
                }
                if (control.equals(Control.PLAY) && (playing || paused)) {
                    if (playing) {
                        player.pause();
                    } else {
                        mediaList.scrollTo(scrollTo);
                        player.play();
                    }
                } else {
                    double volume = 0.5;
                    if (player != null) {
                        volume = player.getVolume();
                        player.dispose();
                    }

                    final MediaPlayer mp = new MediaPlayer(nextInfo.getMedia());
                    mp.setOnReady(new Runnable() {

                        @Override
                        public void run() {
                            nextInfo.setDuration(mp.getTotalDuration());
                        }
                    });
                    mp.setOnEndOfMedia(new Runnable() {

                        @Override
                        public void run() {
                            controlPlayer(Control.NEXT, null);
                        }
                    });

                    {
                        mp.setVolume(volume);
                        controlVolume.valueProperty().unbind();
                        controlVolume.valueProperty().bindBidirectional(mp.volumeProperty());
                    }

                    {
                        final SimpleDoubleProperty totalDuration;
                        {
                            Duration dur = mp.getTotalDuration();
                            if ((dur == null) || dur.equals(Duration.UNKNOWN)) {
                                dur = nextInfo.getDuration();
                            }
                            if ((dur != null) && !dur.equals(Duration.UNKNOWN)) {
                                totalDuration = new SimpleDoubleProperty(dur.toMillis());
                            } else {
                                ObjectProperty<Duration> op = nextInfo.durationProperty();
                                totalDuration = new SimpleDoubleProperty();
                                op.addListener(new WeakChangeListener<>(
                                        new ChangeListener<Duration>() {

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
                        {
                            Double v = totalDuration.getValue();
                            if (v != null) {
                                controlTime.maxProperty().set(v.doubleValue());
                            }
                            controlTime.maxProperty().bind(totalDuration);
                        }
                        //controlTime.valueProperty().unbind();
                        controlTime.setDisable(true);
                        controlTime.valueProperty().addListener(
                                new WeakChangeListener<>(new ChangeListener<Number>() {

                                    @Override
                                    public void changed(
                                            ObservableValue<? extends Number> observable,
                                            Number oldValue, Number newValue) {
                                        if (controlTime.isValueChanging()) {
                                            mp.seek(Duration.millis(controlTime.getValue()));
                                        }
                                    }
                                }));
                        mp.currentTimeProperty().addListener(new InvalidationListener() {

                            private long last;

                            @Override
                            public void invalidated(Observable ov) {
                                final long cv = (long) mp.getCurrentTime().toMillis();
                                if ((last + 100) < cv) {
                                    last = cv;
                                    if (controlTime.isDisable()) {
                                        controlTime.setDisable(false);
                                    }
                                    if (!controlTime.isValueChanging()) {
                                        controlTime.setValue(last);
                                    }
                                }
                            }
                        });
                    }

                    {
                        if (current != null) {
                            current.setCursor(false);
                        }
                        current = nextInfo;
                        current.setCursor(true);
                        player = mp;
                    }

                    mediaList.scrollTo(scrollTo);

                    {
                        image.setImage(null);
                        current.imageProperty().addListener(
                                new WeakChangeListener<>(new ChangeListener<Image>() {

                                    @Override
                                    public void changed(
                                            ObservableValue<? extends Image> observable,
                                            Image oldValue, Image newValue) {
                                        image.setImage(newValue);
                                    }
                                }));
                        Image img = current.imageProperty().get();
                        if (img != null) {
                            image.setImage(img);
                        }
                    }

                    mp.play();
                }
            } finally {
                pLock.unlock();
            }
        }
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
