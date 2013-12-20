package com.brightgenerous.fxplayer.application.playlist;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;

import com.brightgenerous.fxplayer.application.Utils.Inject;
import com.brightgenerous.fxplayer.application.playlist.ImageSaveUtils.Type;
import com.brightgenerous.fxplayer.application.playlist.MediaInfo.MetaChangeListener;

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
    private ToggleButton controlLog;

    private Stage logWindow;

    private TextArea logText;

    // tab - media list

    @FXML
    private TableView<MediaInfo> mediaList;

    @FXML
    private TableColumn<MediaInfo, Boolean> tableColumnIndex;

    @FXML
    private TableColumn<MediaInfo, Boolean> tableColumnCursor;

    @FXML
    private TableColumn<MediaInfo, Duration> tableColumnDuration;

    // tab - video

    @FXML
    private Tab mediaContainer;

    private DoubleBinding mediaViewWidth;

    private DoubleBinding mediaViewHeight;

    @FXML
    private Pane mediaPane;

    // control - button

    @FXML
    private ToggleButton controlPlay;

    // control - time

    @FXML
    private ProgressBar progressCurrent;

    @FXML
    private ProgressBar progressBuffer;

    @FXML
    private Slider controlTime;

    @FXML
    private Label timeText;

    // control - volume

    @FXML
    private Slider controlVolume;

    @FXML
    private Label volumeText;

    // something

    @FXML
    private Pane spectrums;

    @FXML
    private Pane spectrumsWrap;

    @FXML
    private ProgressBar spectrumBar1;

    @FXML
    private ProgressBar spectrumBar2;

    @FXML
    private ProgressBar spectrumBar3;

    @FXML
    private ProgressBar spectrumBar4;

    // dialog

    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    private final FileChooser fileChooser = new FileChooser();

    private final FileChooser saveChooser = new FileChooser();
    {
        File home = null;
        {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                File tmp = new File(userHome);
                if (tmp.exists() && tmp.isDirectory() && tmp.canRead()) {
                    home = tmp;
                }
            }
        }
        if (home != null) {
            directoryChooser.setInitialDirectory(home);
            fileChooser.setInitialDirectory(home);
            saveChooser.setInitialDirectory(home);
        }
        saveChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(ImageSaveUtils.getFileDescription(), ImageSaveUtils
                        .getExtensions()));
    }

    private final ReadWriteLock listLock = new ReentrantReadWriteLock();

    private MediaInfo current;

    private MediaPlayer player;

    private long lastCreate = Long.MIN_VALUE;

    private static final double DEFAULT_VOLUME = 0.25d;

    private static final double SEEK_TIME_DEF = 100; // milliseconds

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        {
            {
                ObjectProperty<Side> side = mediaContainer.getTabPane().sideProperty();
                BooleanBinding sideProp = side.isEqualTo(Side.LEFT).or(side.isEqualTo(Side.RIGHT));
                NumberBinding tabMarginWidth = Bindings.when(sideProp).then(32).otherwise(0);
                NumberBinding tabMarginHeight = Bindings.when(sideProp).then(0).otherwise(32);
                mediaViewWidth = mediaContainer.getTabPane().widthProperty()
                        .subtract(tabMarginWidth);
                mediaViewHeight = mediaContainer.getTabPane().heightProperty()
                        .subtract(tabMarginHeight);
            }

            timeText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        TabPane tabPen = mediaContainer.getTabPane();
                        switch (tabPen.getSide()) {
                            case LEFT:
                            case RIGHT:
                                tabPen.setSide(Side.BOTTOM);
                                break;
                            case TOP:
                            case BOTTOM:
                            default:
                                tabPen.setSide(Side.LEFT);
                                break;
                        }
                    }
                }
            });
        }
        {
            final EventHandler<MouseEvent> clickListener = new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() == 2) {
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
                    TableRow<MediaInfo> ret = new TableRow<MediaInfo>() {

                        @Override
                        protected void updateItem(MediaInfo item, boolean empty) {
                            super.updateItem(item, empty);

                            idProperty().unbind();
                            if (empty) {
                                uninstallMediaInfoTooltips(this);
                            } else {
                                MediaInfo info = getTableView().getItems().get(getIndex());
                                updateMediaInfoTooltip(this, info, 0.9);
                                idProperty().bind(
                                        Bindings.when(info.cursorProperty()).then("row-cursor")
                                                .otherwise((String) null));
                            }
                        }
                    };
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

                                if (empty) {
                                    setText(null);
                                } else {
                                    setText(String.format("%d",
                                            Integer.valueOf(getTableRow().getIndex() + 1)));
                                    setAlignment(Pos.CENTER_RIGHT);
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

                                if (empty || (item == null) || !item.booleanValue()) {
                                    setText(null);
                                } else {
                                    setText(bundle.getString("media.crusor"));
                                    setAlignment(Pos.CENTER);
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

                                if (empty) {
                                    setText(null);
                                } else {
                                    int sec = (int) item.toSeconds();
                                    setText(String.format("%3d:%02d", Integer.valueOf(sec / 60),
                                            Integer.valueOf(sec % 60)));
                                    setAlignment(Pos.CENTER_RIGHT);
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
                if (mp == null) {
                    return;
                }
                if (mp.getStatus().equals(Status.UNKNOWN)) {
                    return;
                }
                double oldMillis = mp.getCurrentTime().toMillis();
                double newMillis = newValue.doubleValue();
                if (SEEK_TIME_DEF < Math.abs(oldMillis - newMillis)) {
                    if (newMillis < mp.getBufferProgressTime().toMillis()) {
                        mp.seek(Duration.millis(newMillis));

                        String oldTime = LabelUtils.milliSecToTime(oldMillis);
                        String newTime = LabelUtils.milliSecToTime(newMillis);
                        if (!oldTime.equals(newTime)) {
                            log("Control Time : old => " + oldTime + " , new => " + newTime);
                        }
                    }
                }
            }
        });
        {
            progressCurrent.prefWidthProperty().bind(controlTime.prefWidthProperty());
            progressCurrent.progressProperty().bind(
                    controlTime.valueProperty().divide(controlTime.maxProperty()));
            progressBuffer.prefWidthProperty().bind(controlTime.prefWidthProperty());
        }

        controlVolume.setValue(DEFAULT_VOLUME);
        controlVolume.valueProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                    Number newValue) {
                player: {
                    MediaPlayer mp = player;
                    if (mp == null) {
                        break player;
                    }
                    if (mp.getStatus().equals(Status.UNKNOWN)) {
                        break player;
                    }
                    double oldVol = mp.getVolume();
                    double newVol = newValue.doubleValue();
                    if (0.01 < Math.abs(oldVol - newVol)) {
                        mp.setVolume(newVol);
                    }
                }
                {
                    String newVol = LabelUtils.toVolume(newValue.doubleValue());
                    if (!newVol.equals(volumeText.getText())) {
                        volumeText.setText(newVol);
                    }
                }
                {
                    int oldVol = (int) (oldValue.doubleValue() * 100);
                    int newVol = (int) (newValue.doubleValue() * 100);
                    if (oldVol != newVol) {
                        log("Control Volume : old => " + oldVol + " , new => " + newVol);
                    }
                }
            }
        });
        volumeText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                int count = event.getClickCount();
                if ((1 < count) && ((count % 2) == 0)) {
                    ObservableList<Node> children = spectrumsWrap.getChildren();
                    if (children.contains(spectrums)) {
                        children.clear();
                    } else {
                        children.add(spectrums);
                    }
                }
            }
        });
        spectrumsWrap.getChildren().clear();

        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if ((event.getClickCount() <= 1) || saveService.isRunning()) {
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
        });

        pathText.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER) && !loadRunning()) {
                    loadHttpService.restart();
                }
            }
        });

        {
            Label label = LabelBuilder
                    .create()
                    .prefWidth(1000)
                    .alignment(Pos.CENTER_RIGHT)
                    .text("brigen fx-player 「Narudake Player」, Copyright(c) 2013 BrightGenerous, All Rights Reserved.")
                    .build();
            logText = TextAreaBuilder.create().wrapText(true).editable(false).build();
            VBox.setVgrow(logText, Priority.ALWAYS);
            Parent parent = VBoxBuilder.create().children(logText, label).build();
            Scene scene = SceneBuilder.create().root(parent).build();
            logWindow = StageBuilder.create().width(640).height(360).scene(scene)
                    .icons(owner.getIcons()).build();
            logWindow.setTitle(bundle.getString("log.title"));
            // logWindow.initOwner(owner);
            logWindow.initModality(Modality.NONE);
            logWindow.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                    new EventHandler<WindowEvent>() {

                        @Override
                        public void handle(WindowEvent event) {
                            controlLog();
                        }
                    });
            owner.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                    new EventHandler<WindowEvent>() {

                        @Override
                        public void handle(WindowEvent event) {
                            logWindow.close();
                        }
                    });
        }

        timeText.setText(LabelUtils.milliSecsToTime(0, 0, 0));
        volumeText.setText(LabelUtils.toVolume(DEFAULT_VOLUME));

        log("Wake up !!");
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
                        if ((title == null) || title.isEmpty()) {
                            saveChooser.setInitialFileName("");
                        } else {
                            Type type = ImageSaveUtils.suggestType();
                            saveChooser.setInitialFileName(ImageSaveUtils.escapeFileName(title
                                    + type.getExtension()));
                        }
                        file = saveChooser.showSaveDialog(owner);
                    }
                    if (file == null) {
                        return Boolean.FALSE;
                    }
                    {
                        File parent = file.getParentFile();
                        if ((parent != null) && parent.isDirectory()) {
                            saveChooser.setInitialDirectory(parent);
                        }
                    }

                    File out = ImageSaveUtils.save(file, image);
                    if (out == null) {

                        log("Save File Failure : " + file.getAbsolutePath());

                        return Boolean.FALSE;
                    }

                    log("Save File : " + out.getAbsolutePath());

                    return Boolean.TRUE;
                }
            };
        }
    };

    @FXML
    protected void controlLog() {
        if (logWindow.isShowing()) {
            logWindow.close();
            controlLog.setSelected(false);
        } else {
            logWindow.show();
            controlLog.setSelected(true);
        }
    }

    @FXML
    protected void controlDirectoryChooser() {
        if (!loadRunning()) {
            loadDirectoryService.restart();
        }
    }

    @FXML
    protected void controlFileChooser() {
        if (!loadRunning()) {
            loadFileService.restart();
        }
    }

    private boolean loadRunning() {
        return loadDirectoryService.isRunning() || loadFileService.isRunning()
                || loadHttpService.isRunning();
    }

    private final MetaChangeListener metaChangeListener = new MetaChangeListener() {

        @Override
        public void onChanged(MediaInfo info, Media media,
                Change<? extends String, ? extends Object> change) {
            String key = change.getKey();

            if (change.wasAdded()) {
                Object value = isOmit(key) ? "...omit..." : change.getValueAdded();
                log("Meta : source => " + info.getDescription() + " , key => " + key
                        + " , value => " + value);
            } else {
                Object value = isOmit(key) ? "...omit..." : change.getValueRemoved();
                log("Meta Remove : source => " + info.getDescription() + " , key => " + key
                        + " , value => " + value);
            }

            if (key.equals("raw metadata")) {
                loadMedia();
            }
        }

        private boolean isOmit(String key) {
            return key.equals("raw metadata") || key.equals("image");
        }
    };

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

                    List<MediaInfo> infos = PlayListReader.fromDirectory(dir, metaChangeListener);

                    if (infos == null) {
                        log("Load Directory Failure : " + dir.getAbsolutePath());
                    } else {
                        log("Load Directory : " + dir.getAbsolutePath());
                    }

                    if (infos == null) {
                        return Boolean.FALSE;
                    }

                    updateItems(dir.toString(), infos);

                    loadMedia();

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

                    List<MediaInfo> infos = PlayListReader.fromFile(file, metaChangeListener);

                    if (infos == null) {
                        log("Load File Failure : " + file.getAbsolutePath());
                    } else {
                        log("Load File : " + file.getAbsolutePath());
                    }

                    if (infos == null) {
                        return Boolean.FALSE;
                    }

                    updateItems(file.toString(), infos);

                    loadMedia();

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

                    List<MediaInfo> infos = PlayListReader.fromURL(text, metaChangeListener);

                    if (infos == null) {
                        log("Load URL Failure : " + text);
                    } else {
                        log("Load URL : " + text);
                    }

                    if (infos == null) {
                        return Boolean.FALSE;
                    }

                    updateItems(text, infos);

                    loadMedia();

                    return Boolean.TRUE;
                }
            };
        }
    };

    private void loadMedia() {
        if (Platform.isFxApplicationThread()) {
            if (loadMediaService.isRunning()) {
                loadMediaService.cancel();
            }
            loadMediaService.restart();
        } else {
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
    }

    @FXML
    protected void controlPlay() {
        boolean selected = controlPlay.isSelected();
        if (!controlPlayer(Control.PLAY, null)) {
            controlPlay.setSelected(!selected);
        }
    }

    @FXML
    protected void controlPrev() {
        controlPlayer(Control.PREV, null);
    }

    @FXML
    protected void controlNext() {
        controlPlayer(Control.NEXT, null);
    }

    private boolean controlPlayer(Control control, MediaInfo info) {
        return controlPlayer(control, info, false);
    }

    private enum Control {
        PLAY, PREV, NEXT, SPECIFY;
    }

    private boolean controlPlayer(Control control, MediaInfo info, boolean chain) {

        // if too fast to be called repeatedly,
        //   returns false.
        boolean ret = true;

        player_block: {

            if (control.equals(Control.PLAY)) {
                boolean playing = false;
                boolean paused = false;
                if (player != null) {
                    Status status = player.getStatus();
                    playing = status.equals(Status.PLAYING);
                    paused = status.equals(Status.PAUSED);
                }
                if (playing) {

                    log("Control Pause : " + current.getDescription());

                    player.pause();

                    break player_block;
                } else if (paused) {
                    if (current == null) {

                        // when paused is true, means player is not null.
                        //   then current would not be null.

                        log("WARNING!!! : current is null");

                        break player_block;
                    }

                    List<MediaInfo> items = getItemsSnapshot();
                    int scrollTo = items.indexOf(current);
                    if (scrollTo != -1) {
                        requestScroll(scrollTo);
                    }

                    log("Control Resume : " + current.getDescription());

                    player.play();

                    break player_block;
                }
            }

            if (!chain) {
                // OMAJINAI!!!
                long currentTime = System.currentTimeMillis();
                if (currentTime < (lastCreate + 500)) {

                    ret = false;

                    break player_block;
                }
                lastCreate = currentTime;
            }

            // now loading ...
            {
                if (player != null) {
                    MediaException me = player.getError();
                    boolean unknown = player.getStatus().equals(Status.UNKNOWN);
                    if (unknown && (me == null)) {

                        log("Now Loading ... Please wait a minute.");

                        ret = false;

                        break player_block;
                    }
                }
            }

            final MediaInfo targetInfo;
            final int scrollTo;
            {
                List<MediaInfo> items = getItemsSnapshot();
                if (items.isEmpty()) {

                    viewStop();

                    break player_block;
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

            log("Control Request : " + targetInfo.getDescription());

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

            if (player != null) {
                player.dispose();
            }

            mp.setOnReady(new Runnable() {

                @Override
                public void run() {
                    Duration dur = mp.getTotalDuration();
                    if ((dur != null) && !dur.isUnknown()) {
                        targetInfo.durationProperty().setValue(dur);
                    }

                    {
                        // this callback would run on main thread.
                        //   so here, (targetInfo == current) is Deterministic ?
                        if (targetInfo != current) {

                            log("WARNING!!! : targetInfo != current");

                            mp.dispose();
                            return;
                        }
                    }

                    // reset seek 
                    {
                        // (should be before set max shorter)
                        controlTime.setValue(0);
                    }

                    // time max
                    {
                        final SimpleDoubleProperty totalDuration;
                        {
                            if ((dur == null) || dur.equals(Duration.UNKNOWN)) {
                                dur = targetInfo.durationProperty().getValue();
                            }
                            if ((dur != null) && !dur.equals(Duration.UNKNOWN)) {
                                totalDuration = new SimpleDoubleProperty(dur.toMillis());
                            } else {
                                totalDuration = new SimpleDoubleProperty();
                                targetInfo.durationProperty().addListener(
                                        new ChangeListener<Duration>() {

                                            @Override
                                            public void changed(
                                                    ObservableValue<? extends Duration> observable,
                                                    Duration oldValue, Duration newValue) {
                                                if (targetInfo == current) {
                                                    totalDuration.set(newValue.toMillis());
                                                }
                                                targetInfo.durationProperty().removeListener(this);
                                            }
                                        });
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
                                public void invalidated(Observable observable) {
                                    if (targetInfo != current) {
                                        return;
                                    }
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
                                if (targetInfo != current) {
                                    return;
                                }
                                if (controlTime.isValueChanging()) {
                                    return;
                                }
                                double newMillis = newValue.toMillis();
                                if (SEEK_TIME_DEF < Math.abs(controlTime.getValue() - newMillis)) {
                                    controlTime.setValue(newMillis);
                                }
                                String newText = LabelUtils.milliSecsToTime(newMillis, mp
                                        .getTotalDuration().toMillis(), mp.getBufferProgressTime()
                                        .toMillis());
                                if (!newText.equals(timeText.getText())) {
                                    timeText.setText(newText);
                                }
                            }
                        });
                    }

                    // time buffer
                    {
                        mp.bufferProgressTimeProperty().addListener(new ChangeListener<Duration>() {

                            @Override
                            public void changed(ObservableValue<? extends Duration> observable,
                                    Duration oldValue, Duration newValue) {
                                if (targetInfo != current) {
                                    return;
                                }
                                double newMillis = newValue.toMillis();
                                String newText = LabelUtils.milliSecsToTime(mp.getCurrentTime()
                                        .toMillis(), mp.getTotalDuration().toMillis(), newMillis);
                                if (!newText.equals(timeText.getText())) {
                                    timeText.setText(newText);
                                }
                                progressBuffer.setProgress(newMillis
                                        / mp.getTotalDuration().toMillis());
                            }
                        });
                        progressBuffer.setProgress(mp.getBufferProgressTime().toMillis()
                                / mp.getTotalDuration().toMillis());
                    }

                    // volume
                    {
                        mp.setVolume(controlVolume.getValue());
                    }

                    // image
                    {
                        targetInfo.imageProperty().addListener(new ChangeListener<Image>() {

                            @Override
                            public void changed(ObservableValue<? extends Image> observable,
                                    Image oldValue, Image newValue) {
                                if (targetInfo == current) {
                                    imageView.setImage(newValue);
                                    requestScroll(scrollTo);
                                }
                                targetInfo.imageProperty().removeListener(this);
                            }
                        });
                        imageView.setImage(targetInfo.imageProperty().get());
                        requestScroll(scrollTo);
                    }

                    // something
                    {
                        mp.setAudioSpectrumInterval(0.3);
                        mp.setAudioSpectrumNumBands(0);
                        mp.setAudioSpectrumListener(new AudioSpectrumListener() {

                            @Override
                            public void spectrumDataUpdate(double timestamp, double duration,
                                    float[] magnitudes, float[] phases) {
                                spectrumBar1.setProgress((magnitudes[0] + 60) / 60);
                                spectrumBar2.setProgress((magnitudes[1] + 60) / 60);
                                spectrumBar3.setProgress(phases[0] / Math.PI);
                                spectrumBar4.setProgress(phases[1] / Math.PI);
                            }
                        });
                    }

                    // setup view
                    if ((0 < mp.getMedia().getWidth()) || (0 < mp.getMedia().getHeight())) {
                        MediaView mediaView = createMedieView(mp, targetInfo);
                        {
                            ObservableList<Node> childrens = mediaPane.getChildren();
                            for (Node node : childrens) {
                                if (node instanceof MediaView) {
                                    destroyMediaView((MediaView) node);
                                }
                            }
                            childrens.clear();
                            childrens.add(mediaView);
                        }

                        mediaContainer.setText(LabelUtils.toTabLabel(targetInfo.getDescription()));
                    } else {
                        mediaPane.getChildren().clear();

                        mediaContainer.setText(bundle.getString("tab.video"));
                    }

                    // play
                    {
                        mp.play();

                        log("Control Play : " + targetInfo.getDescription());
                    }
                }
            });

            mp.setOnError(new Runnable() {

                @Override
                public void run() {
                    {
                        // OMAJINAI!!!
                        layoutTimer();
                    }
                    imageView.setImage(new Image(PlayList.class.getResourceAsStream("dame.png")));

                    viewStop();
                }
            });

            mp.setOnPlaying(new Runnable() {

                @Override
                public void run() {
                    viewPlaying();
                }
            });

            mp.setOnPaused(new Runnable() {

                @Override
                public void run() {
                    viewStop();
                }
            });

            mp.setOnEndOfMedia(new Runnable() {

                @Override
                public void run() {

                    log("End of Media : " + targetInfo.getDescription());

                    // no fear, would be called on main thread.
                    controlPlayer(Control.NEXT, null);
                }
            });

            // cursor
            {
                if (current != null) {
                    current.cursorProperty().setValue(Boolean.FALSE);
                }
                targetInfo.cursorProperty().setValue(Boolean.TRUE);
            }

            // tab
            {
                if ((0 < targetInfo.getWidth()) || (0 < targetInfo.getHeight())) {
                    mediaContainer.setText(LabelUtils.toTabLabel(targetInfo.getDescription()));
                } else {
                    mediaContainer.setText(bundle.getString("tab.video"));
                }
            }

            current = targetInfo;
            player = mp;
        }

        return ret;
    }

    private void viewPlaying() {
        controlPlay.setSelected(true);
    }

    private void viewStop() {
        controlPlay.setSelected(false);
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

    private void updateItems(String path, List<MediaInfo> items) {
        Lock lock = listLock.writeLock();
        try {
            lock.lock();
            pathText.setText(path);
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

                    Thread.yield();

                    List<MediaInfo> items = getItemsSnapshot();
                    int i = 0;
                    for (; i < items.size(); i++) {
                        MediaInfo info = items.get(i);
                        if (!info.loaded()) {
                            if (!isCancelled()) {
                                if (info.load()) {

                                    // expect chain loading.
                                    //   would be called from meta data loaded callback.

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
        if (row < 0) {
            return;
        }
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

    private MediaView createMedieView(MediaPlayer player, MediaInfo info) {
        MediaView mediaView = new MediaView(player);
        mediaView.setSmooth(true);
        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(mediaViewWidth);
        mediaView.fitHeightProperty().bind(mediaViewHeight);
        mediaView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 1) {
                    controlPlay();
                }
            }
        });

        installMediaInfoTooltip(mediaView, info, 0.8);

        return mediaView;
    }

    private void destroyMediaView(MediaView mediaView) {
        mediaView.fitWidthProperty().unbind();
        mediaView.fitHeightProperty().unbind();

        uninstallMediaInfoTooltips(mediaView);
    }

    private static Tooltip updateMediaInfoTooltip(Node node, MediaInfo info, double opacity) {
        uninstallMediaInfoTooltips(node);
        return installMediaInfoTooltip(node, info, opacity);
    }

    private static Tooltip installMediaInfoTooltip(Node node, MediaInfo info, final double opacity) {
        final Tooltip ret = new Tooltip();
        {
            ret.textProperty().bind(info.tooltipProperty());
            final ImageView imageView = new ImageView();
            {
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setFitWidth(0);
                imageView.setFitHeight(0);
                imageView.imageProperty().addListener(new ChangeListener<Image>() {

                    @Override
                    public void changed(ObservableValue<? extends Image> observable,
                            Image oldValue, Image newValue) {
                        if (newValue == null) {
                            imageView.setFitWidth(0);
                            imageView.setFitHeight(0);
                        } else {
                            imageView.setFitWidth(50);
                            imageView.setFitHeight(50);
                        }
                    }
                });
                imageView.imageProperty().bind(info.imageProperty());
            }
            ret.setGraphic(imageView);
            ret.setStyle("-fx-background-color:linear-gradient(cyan,deepskyblue);-fx-padding: 5 15 5 5;");
        }

        node.setUserData(ret);
        Tooltip.install(node, ret);

        final ObservableValue<Boolean> visible = info.visibleTooltipProperty();
        if (visible.getValue().booleanValue()) {
            ret.setOpacity(opacity);
        } else {
            ret.setOpacity(0);
            visible.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    if (newValue.booleanValue()) {
                        ret.setOpacity(opacity);
                    } else {
                        ret.setOpacity(0);
                    }
                    visible.removeListener(this);
                }
            });
        }

        return ret;
    }

    private static void uninstallMediaInfoTooltips(Node node) {
        Tooltip tooltip = null;
        {
            Object data = node.getUserData();
            if (data instanceof Tooltip) {
                tooltip = (Tooltip) data;
            }
        }
        if (tooltip != null) {
            tooltip.textProperty().unbind();
            {
                Node g = tooltip.getGraphic();
                if (g instanceof ImageView) {
                    ((ImageView) g).imageProperty().unbind();
                }
            }
            Tooltip.uninstall(node, tooltip);
            node.setUserData(null);
        }
    }

    //----------------------------------------------------------------------------------
    // About log type.
    //
    // i will implement this, i will do...
    //   however, in this time, 
    //   it has not been designated that the specified version and commit is what.
    //
    // watashi ga sonoki ni nareba,
    //   jissou ha 10 nengo 20 nengo to iukoto mo kanou ...
    //-------------------------

    // @formatter:off
    private enum LogType {
        Log, Warn,
        Load,
        Control_Time, Control_Volume,
        Control_Request, Control_Play, Control_Pause, Control_Resume
    }
    // @formatter:on

    private void log(String str) {
        log(LogType.Log, str);
    }

    private void log(LogType type, String str) {
        logText.appendText(String.format("%1$tH:%1$tM:%1$tS:%1$tL - %2$s%n", new Date(), str));
    }
}
