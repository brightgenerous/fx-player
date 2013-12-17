package com.brightgenerous.fxplayer.application.playlist;

import java.io.File;
import java.io.IOException;
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
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import com.brightgenerous.fxplayer.application.Utils.Inject;

public class PlayList implements Initializable {

    @Inject
    private Stage owner;

    @Inject
    private ResourceBundle bundle;

    @FXML
    private ImageView imageView;

    @FXML
    private TextField pathText;

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
    private Text timeText;

    @FXML
    private Slider controlVolume;

    @FXML
    private Text volumeText;

    @FXML
    private ProgressBar progressBar1;

    @FXML
    private ProgressBar progressBar2;

    @FXML
    private ProgressBar progressBar3;

    @FXML
    private ProgressBar progressBar4;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    private final FileChooser fileChooser = new FileChooser();

    private final FileChooser saveChooser = new FileChooser();
    {
        File home = null;
        {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                File tmp = new File(userHome);
                if (tmp.exists() && tmp.isDirectory()) {
                    home = tmp;
                }
            }
        }
        if (home != null) {
            directoryChooser.setInitialDirectory(home);
            fileChooser.setInitialDirectory(home);
            saveChooser.setInitialDirectory(home);
        }
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png*"));
    }

    private final ReadWriteLock listLock = new ReentrantReadWriteLock();

    private MediaInfo current;

    private MediaPlayer player;

    private long lastCreate = Long.MIN_VALUE;

    private static final double DEFAULT_VOLUME = 0.5d;

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

        controlTime.valueProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                    Number newValue) {
                MediaPlayer mp = player;
                if ((mp != null) && (controlTime.isValueChanging())) {
                    mp.seek(Duration.millis(newValue.doubleValue()));
                }
            }
        });

        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (1 < event.getClickCount()) {
                    if (saveService.isRunning()) {
                        return;
                    }
                    Object source = event.getSource();
                    if (source instanceof ImageView) {
                        ImageView imageView = (ImageView) source;
                        final Image image = imageView.getImage();
                        if (image != null) {
                            if (!saveService.isRunning()) {
                                saveService.restart();
                            }
                        }
                    }
                }
            }
        });
        imageView.imageProperty().addListener(new ChangeListener<Image>() {

            @Override
            public void changed(ObservableValue<? extends Image> observable, Image oldValue,
                    Image newValue) {
                if (newValue == null) {
                    imageView.setCursor(Cursor.DEFAULT);
                } else {
                    imageView.setCursor(Cursor.HAND);
                }
            }
        });

        pathText.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    if (!loadHttpService.isRunning()) {
                        loadHttpService.restart();
                    }
                }
            }
        });
    }

    private final Service<Boolean> saveService = new Service<Boolean>() {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    MediaInfo info = current;
                    if (info == null) {
                        return Boolean.FALSE;
                    }

                    Image image = info.imageProperty().getValue();
                    if (image == null) {
                        return Boolean.FALSE;
                    }

                    File file;
                    {
                        String title = info.titleProperty().getValue();
                        if (title == null) {
                            saveChooser.setInitialFileName("");
                        } else {
                            saveChooser.setInitialFileName(title.replaceAll(
                                    "[\\s\\\\/:*?\"<>\\|]{1}", "_") + ".png");
                        }
                        file = saveChooser.showSaveDialog(owner);
                        if (file == null) {
                            return Boolean.FALSE;
                        }
                        {
                            File parent = file.getParentFile();
                            if ((parent != null) && parent.isDirectory()) {
                                saveChooser.setInitialDirectory(parent);
                            }
                        }
                    }

                    File out;
                    out: {
                        String base = file.getAbsolutePath();
                        if (base.endsWith(".png")) {
                            // here
                            // when over write, must be confirmed.
                            //   or create the file.
                            out = file;
                            break out;
                        }
                        base = base.replaceAll("\\.png$", "");
                        String name;
                        int index = 0;
                        do {
                            if (index == 0) {
                                name = base;
                            } else {
                                name = base + "_" + index;
                            }
                            index++;
                            out = new File(name + ".png");
                        } while (out.exists());
                    }
                    try {
                        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", out);
                    } catch (IOException e) {
                    }
                    return Boolean.TRUE;
                }
            };
        }
    };

    @FXML
    protected void controlDirectoryChooser() {
        if (!loadDirectoryService.isRunning()) {
            loadDirectoryService.restart();
        }
    }

    @FXML
    protected void controlFileChooser() {
        if (!loadFileService.isRunning()) {
            loadFileService.restart();
        }
    }

    private final Service<Boolean> loadDirectoryService = new Service<Boolean>() {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    File dir = directoryChooser.showDialog(owner);
                    if (dir == null) {
                        return Boolean.FALSE;
                    }
                    {
                        File parent = dir.getParentFile();
                        if ((parent != null) && parent.isDirectory()) {
                            directoryChooser.setInitialDirectory(parent);
                        } else {
                            directoryChooser.setInitialDirectory(dir);
                        }
                    }

                    List<MediaInfo> infos = PlayListReader.fromDirectory(dir, loadMediaService);
                    if (infos == null) {
                        return Boolean.FALSE;
                    }

                    updateItems(dir.toString(), infos);

                    loadMediaLater();

                    return Boolean.TRUE;
                }
            };
        }
    };

    private final Service<Boolean> loadFileService = new Service<Boolean>() {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    File file = fileChooser.showOpenDialog(owner);
                    if (file == null) {
                        return Boolean.FALSE;
                    }
                    {
                        File parent = file.getParentFile();
                        if ((parent != null) && parent.isDirectory()) {
                            fileChooser.setInitialDirectory(parent);
                        }
                    }

                    List<MediaInfo> infos = PlayListReader.fromFile(file, loadMediaService);
                    if (infos == null) {
                        return Boolean.FALSE;
                    }

                    updateItems(file.toString(), infos);

                    loadMediaLater();

                    return Boolean.TRUE;
                }
            };
        }
    };

    private final Service<Boolean> loadHttpService = new Service<Boolean>() {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    String text = pathText.getText().trim();

                    List<MediaInfo> infos = PlayListReader.fromURL(text, loadMediaService);
                    if (infos == null) {
                        return Boolean.FALSE;
                    }

                    updateItems(text, infos);

                    loadMediaLater();

                    return Boolean.TRUE;
                }
            };
        }
    };

    private void loadMediaLater() {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                if (loadMediaService.isRunning()) {
                    loadMediaService.cancel();
                }
                loadMediaService.restart();
            }
        });
    }

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
        controlPlayer(control, info, false);
    }

    private enum Control {
        PLAY, PREV, NEXT, SPECIFY;
    }

    private void controlPlayer(Control control, MediaInfo info, boolean chain) {
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

            if (!chain) {
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

            final MediaPlayer mp;
            try {
                mp = new MediaPlayer(targetInfo.getMedia());
            } catch (MediaLoadException e) {
                onErrorLoadMedia(targetInfo);
                if (!chain) {
                    final Control ctrl = control;
                    Platform.runLater(new Runnable() {

                        @Override
                        public void run() {
                            if (ctrl.equals(Control.PREV)) {
                                controlPlayer(Control.PREV, targetInfo, true);
                            } else {
                                controlPlayer(Control.NEXT, targetInfo, true);
                            }
                        }
                    });
                }
                break player_block;
            }

            final double volume;
            if (player == null) {
                volume = DEFAULT_VOLUME;
            } else {
                if (player.getStatus().equals(Status.UNKNOWN)) {
                    volume = DEFAULT_VOLUME;
                } else {
                    volume = player.getVolume();
                }
                player.dispose();
            }

            mp.setOnError(new Runnable() {

                @Override
                public void run() {
                    {
                        // OMAJINAI!!!
                        layoutTimer();
                    }
                    imageView.setImage(new Image(PlayList.class.getResourceAsStream("dame.png")));
                }
            });
            mp.setOnReady(new Runnable() {

                @Override
                public void run() {
                    Duration dur = mp.getTotalDuration();
                    if ((dur != null) && !dur.isUnknown()) {
                        targetInfo.durationProperty().setValue(dur);
                    }

                    // volume
                    {
                        mp.setVolume(volume);
                        controlVolume.valueProperty().bindBidirectional(mp.volumeProperty());
                    }

                    // something
                    {
                        mp.setAudioSpectrumInterval(0.3);
                        mp.setAudioSpectrumNumBands(0);
                        mp.setAudioSpectrumListener(new AudioSpectrumListener() {

                            @Override
                            public void spectrumDataUpdate(double arg0, double arg1, float[] arg2,
                                    float[] arg3) {
                                progressBar1.setProgress((arg2[0] + 60) / 60);
                                progressBar2.setProgress((arg2[1] + 60) / 60);
                                progressBar3.setProgress(arg3[0] / Math.PI);
                                progressBar4.setProgress(arg3[1] / Math.PI);
                            }
                        });
                    }

                    // play
                    {
                        mp.play();
                    }
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
                controlVolume.valueProperty().unbind();
                mp.volumeProperty().addListener(new ChangeListener<Number>() {

                    @Override
                    public void changed(ObservableValue<? extends Number> observable,
                            Number oldValue, Number newValue) {
                        int vol = (int) (newValue.doubleValue() * 100);
                        volumeText.setText(String.format("%3d%%", Integer.valueOf(vol)));
                    }
                });
                volumeText.setText(String.format("%3d%%", Integer.valueOf((int) (volume * 100))));
            }

            {
                // time max
                {
                    final SimpleDoubleProperty totalDuration;
                    {
                        Duration dur = mp.getTotalDuration();
                        if ((dur == null) || dur.equals(Duration.UNKNOWN)) {
                            dur = targetInfo.durationProperty().getValue();
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
                        prepareLayoutTimer();
                        totalDuration.addListener(new InvalidationListener() {

                            @Override
                            public void invalidated(Observable arg0) {
                                layoutTimer();
                            }
                        });
                    }
                    controlTime.maxProperty().bind(totalDuration);
                }

                // time current
                {
                    mp.currentTimeProperty().addListener(new ChangeListener<Duration>() {

                        @Override
                        public void changed(ObservableValue<? extends Duration> observable,
                                Duration oldValue, Duration newValue) {
                            if (!controlTime.isValueChanging()) {
                                double millis = newValue.toMillis();
                                controlTime.setValue(millis);
                                int sec = (int) (millis / 1000);
                                timeText.setText(String.format("%3d:%02d",
                                        Integer.valueOf(sec / 60), Integer.valueOf(sec % 60)));
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
                    current.cursorProperty().setValue(Boolean.FALSE);
                }
                targetInfo.cursorProperty().setValue(Boolean.TRUE);
            }

            current = targetInfo;
            player = mp;
        }
    }

    private void prepareLayoutTimer() {
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

    private void layoutTimer() {
        Object obj = controlTime.getUserData();
        if (obj instanceof Double) {
            double opacity = ((Double) obj).doubleValue();
            if (controlTime.getOpacity() != opacity) {
                controlTime.setOpacity(opacity);
            }
        }
    }

    private void updateItems(String text, List<MediaInfo> items) {
        Lock lock = listLock.writeLock();
        try {
            lock.lock();
            pathText.setText(text);
            mediaList.getItems().clear();
            mediaList.getItems().addAll(items);
        } finally {
            lock.unlock();
        }
    }

    private final Service<Boolean> loadMediaService = new Service<Boolean>() {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    List<MediaInfo> items = getItemsSnapshot();
                    int i = 0;
                    for (; i < items.size(); i++) {
                        MediaInfo info = items.get(i);
                        if (!info.loaded()) {
                            if (!isCancelled()) {
                                if (info.load()) {
                                    // expect chain loading
                                    break;
                                }
                                onErrorLoadMedia(info);
                                Thread.yield();
                            }
                        }
                    }
                    return Boolean.TRUE;
                }
            };
        }
    };

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

    private void onErrorLoadMedia(MediaInfo info) {
    }

    private void requestScroll(final int row) {
        if (0 <= row) {
            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    Lock lLock = listLock.readLock();
                    try {
                        lLock.lock();
                        int size = mediaList.getItems().size();
                        if (row < size) {
                            mediaList.getSelectionModel().select(row);
                            mediaList.scrollTo(row);
                        }
                    } finally {
                        lLock.unlock();
                    }
                }
            });
        }
    }
}
