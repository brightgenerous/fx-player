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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;

import com.brightgenerous.fxplayer.application.FxUtils;
import com.brightgenerous.fxplayer.application.FxUtils.Inject;
import com.brightgenerous.fxplayer.application.playlist.VideoPane.InfoSide;
import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.media.MediaLoadException;
import com.brightgenerous.fxplayer.media.MediaStatus;
import com.brightgenerous.fxplayer.service.LoadDirectoryService;
import com.brightgenerous.fxplayer.service.LoadFileService;
import com.brightgenerous.fxplayer.service.LoadUrlService;
import com.brightgenerous.fxplayer.service.SaveImageService;
import com.brightgenerous.fxplayer.service.StageCloseService;

public class PlayList implements Initializable {

    private final Settings settings = new Settings();

    @Inject
    private Stage stage;

    private final ObjectProperty<Stage> stageProperty = new SimpleObjectProperty<>();

    @Inject
    private ResourceBundle bundle;

    @FXML
    private Pane rootPane;

    // top control

    @FXML
    private TextField pathText;

    private final Property<String> pathTextProperty = new SimpleStringProperty();

    // log

    @FXML
    private ToggleButton controlLog;

    private LoggStage logStage;

    // tab - info

    @FXML
    private TableView<MediaInfo> infoList;

    @FXML
    private TableColumn<MediaInfo, MediaStatus> infoListColumnIndex;

    @FXML
    private TableColumn<MediaInfo, Duration> infoListColumnDuration;

    // tab - video

    @FXML
    private Tab videoTab;

    private DoubleBinding mediaViewWidth;

    private DoubleBinding mediaViewHeight;

    @FXML
    private VideoPane videoPane;

    @FXML
    private TableView<MediaInfo> infoListClone;

    @FXML
    private TableColumn<MediaInfo, MediaStatus> infoListCloneColumnIndex;

    @FXML
    private TableColumn<MediaInfo, Duration> infoListCloneColumnDuration;

    // controls

    @FXML
    private GridPane timesVolumesPane;

    @FXML
    private Pane timesPane;

    @FXML
    private Pane volumesPane;

    // control - button

    @FXML
    private ToggleButton controlPlayPause;

    // control - time

    @FXML
    private Slider controlTime;

    @FXML
    private ProgressBar controlTimeCurrent;

    @FXML
    private ProgressBar controlTimeBuffer;

    @FXML
    private Label timeText;

    // control - volume

    @FXML
    private Slider controlVolume;

    @FXML
    private Label volumeText;

    // image

    @FXML
    private ImageView imageView;

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

    // state properties

    private final ObjectProperty<MediaInfo> currentInfoProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<MediaPlayer> playerProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<Boolean> directionProperty = new SimpleObjectProperty<>();

    private final LongProperty playerCreateTimeProperty = new SimpleLongProperty(Long.MIN_VALUE);

    private final ReadWriteLock infoListLock = new ReentrantReadWriteLock();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        {
            stageProperty.setValue(stage);
            pathTextProperty.bind(pathText.textProperty());
        }

        // tab
        {
            videoTab.getTabPane().addEventHandler(MouseEvent.MOUSE_CLICKED,
                    new EventHandler<MouseEvent>() {

                        @Override
                        public void handle(MouseEvent event) {
                            int count = event.getClickCount();
                            if ((1 < count) && ((count % 2) == 0)) {
                                EventTarget target = event.getTarget();
                                if (!(target instanceof StackPane)) {
                                    return;
                                }
                                List<String> scs = ((Node) target).getStyleClass();
                                // JavaFX 2.2 style class name "tab-header-background"
                                if ((scs == null) || !scs.contains("tab-header-background")) {
                                    return;
                                }
                                settings.toggleVisibleVideoInfoSide();
                            }
                        }
                    });

            settings.visibleVideoInfo.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    boolean nv = newValue.booleanValue();
                    if (nv != videoPane.getVisibleInfoList()) {
                        videoPane.setVisibleInfoList(nv);
                    }
                }
            });

            settings.videoInfoSide.addListener(new ChangeListener<InfoSide>() {

                @Override
                public void changed(ObservableValue<? extends InfoSide> observable,
                        InfoSide oldValue, InfoSide newValue) {
                    videoPane.setInfoSide(newValue);
                }
            });
        }

        // video
        {
            mediaViewWidth = videoTab.getTabPane().widthProperty()
                    .subtract(settings.tabMarginWidth);
            mediaViewHeight = videoTab.getTabPane().heightProperty()
                    .subtract(settings.tabMarginHeight);

            videoTab.getTabPane().sideProperty().bind(settings.tabSide);
            timeText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        settings.toggleTabSide();
                    }
                }
            });
        }

        // info list
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
                                controlPlayerForceWithoutSkip(Control.SPECIFY, (MediaInfo) obj);
                            }
                        }
                    }
                }
            };
            infoList.setRowFactory(new Callback<TableView<MediaInfo>, TableRow<MediaInfo>>() {

                @Override
                public TableRow<MediaInfo> call(TableView<MediaInfo> param) {
                    TableRow<MediaInfo> ret = new TableRow<MediaInfo>() {

                        @Override
                        protected void updateItem(MediaInfo item, boolean empty) {
                            super.updateItem(item, empty);

                            MediaInfo info = null;
                            {
                                int idx = getIndex();
                                ObservableList<MediaInfo> items = getTableView().getItems();
                                if ((items != null) && (0 <= idx) && (idx < items.size())) {
                                    info = items.get(idx);
                                }
                            }
                            idProperty().unbind();
                            if (empty || (info == null)) {
                                uninstallMediaInfoTooltips(this);
                                idProperty().setValue(null);
                            } else {
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

            infoListColumnIndex
                    .setCellFactory(new Callback<TableColumn<MediaInfo, MediaStatus>, TableCell<MediaInfo, MediaStatus>>() {

                        @Override
                        public TableCell<MediaInfo, MediaStatus> call(
                                TableColumn<MediaInfo, MediaStatus> param) {
                            return new TableCell<MediaInfo, MediaStatus>() {

                                @Override
                                protected void updateItem(MediaStatus item, boolean empty) {
                                    super.updateItem(item, empty);

                                    if (empty || (item == null)) {
                                        setText(null);
                                    } else {
                                        String str;
                                        switch (item) {
                                            case MEDIA_YET:
                                                str = String.format("- %d", Integer
                                                        .valueOf(getTableRow().getIndex() + 1));
                                                break;
                                            case MEDIA_SUCCESS:
                                                str = String.format("%d", Integer
                                                        .valueOf(getTableRow().getIndex() + 1));
                                                break;
                                            case MEDIA_ERROR:
                                                str = String.format("x %d", Integer
                                                        .valueOf(getTableRow().getIndex() + 1));
                                                break;
                                            case PLAYER_LOADING:
                                            case PLAYER_READY:
                                            case PLAYER_PLAYING:
                                            case PLAYER_PAUSE:
                                                str = String.format("+ %d", Integer
                                                        .valueOf(getTableRow().getIndex() + 1));
                                                break;
                                            case PLAYER_END:
                                                str = String.format("%d", Integer
                                                        .valueOf(getTableRow().getIndex() + 1));
                                                break;
                                            default:
                                                str = "";
                                                break;
                                        }
                                        setText(str);
                                        setAlignment(Pos.CENTER_RIGHT);
                                    }
                                }
                            };
                        }
                    });

            infoListColumnDuration
                    .setCellFactory(new Callback<TableColumn<MediaInfo, Duration>, TableCell<MediaInfo, Duration>>() {

                        @Override
                        public TableCell<MediaInfo, Duration> call(
                                TableColumn<MediaInfo, Duration> param) {

                            return new TableCell<MediaInfo, Duration>() {

                                @Override
                                protected void updateItem(Duration item, boolean empty) {
                                    super.updateItem(item, empty);

                                    if (empty || (item == null)) {
                                        setText(null);
                                    } else {
                                        setText(LabelUtils.milliSecToTime(item.toMillis()));
                                        setAlignment(Pos.CENTER_RIGHT);
                                    }
                                }
                            };
                        }
                    });

            // clone
            infoListClone.setRowFactory(infoList.getRowFactory());
            infoListCloneColumnIndex.setCellFactory(infoListColumnIndex.getCellFactory());
            infoListCloneColumnDuration.setCellFactory(infoListColumnDuration.getCellFactory());
        }

        // control times volumes
        {
            settings.timesVolumesHorizontal.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    timesVolumesPane.getChildren().clear();
                    if (newValue.booleanValue()) {
                        timesVolumesPane.add(timesPane, 1, 0);
                        timesVolumesPane.add(volumesPane, 0, 0);
                    } else {
                        timesVolumesPane.add(timesPane, 0, 0);
                        timesVolumesPane.add(volumesPane, 0, 1);
                    }
                }
            });
        }

        // control time
        {
            controlTime.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                        Number newValue) {
                    MediaPlayer mp = playerProperty.getValue();
                    if (mp == null) {
                        return;
                    }
                    if (mp.getStatus() == Status.UNKNOWN) {
                        return;
                    }
                    double oldMillis = mp.getCurrentTime().toMillis();
                    double newMillis = newValue.doubleValue();
                    if (settings.thresholdTime(oldMillis, newMillis)) {
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

            controlTimeCurrent.prefWidthProperty().bind(controlTime.prefWidthProperty());
            controlTimeCurrent.progressProperty().bind(
                    controlTime.valueProperty().divide(controlTime.maxProperty()));
            controlTimeBuffer.prefWidthProperty().bind(controlTime.prefWidthProperty());
        }

        // control volume
        {
            controlVolume.setValue(Settings.DEFAULT_VOLUME); // set before add listener, for avoid log.
            controlVolume.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                        Number newValue) {
                    player: {
                        MediaPlayer mp = playerProperty.getValue();
                        if (mp == null) {
                            break player;
                        }
                        if (mp.getStatus() == Status.UNKNOWN) {
                            break player;
                        }
                        double oldVol = mp.getVolume();
                        double newVol = newValue.doubleValue();
                        if (0.01 <= Math.abs(oldVol - newVol)) {
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
        }

        // something
        {
            settings.visibleSpectrums.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    ObservableList<Node> children = spectrumsWrap.getChildren();
                    if (newValue.booleanValue()) {
                        if (!children.contains(spectrums)) {
                            children.add(spectrums);
                        }
                    } else {
                        if (children.contains(spectrums)) {
                            children.remove(spectrums);
                        }
                    }
                }
            });
            volumeText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        settings.toggleVisibleSpectrums();
                    }
                }
            });
        }

        // image
        {
            imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() <= 1) {
                        return;
                    }
                    if (!saveImageService.isRunning()) {
                        saveImageService.restart();
                    }
                }
            });
        }

        // path
        {
            pathText.setOnKeyPressed(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.ENTER) {
                        if (!loadRunning()) {
                            loadUrlService.restart();
                        }
                    }
                }
            });
        }

        // log
        {
            logStage = new LoggStage(bundle.getString("log.title"), stage.getIcons(),
                    shortCutHandler);
            logStage.setOnHidden(new EventHandler<WindowEvent>() {

                @Override
                public void handle(WindowEvent event) {
                    controlLog.setSelected(false);
                    logStage.setX(logStage.getX());
                    logStage.setY(logStage.getY());
                }
            });
            logStage.setOnShown(new EventHandler<WindowEvent>() {

                @Override
                public void handle(WindowEvent event) {
                    controlLog.setSelected(true);
                }
            });
        }

        // exit event
        {
            rootPane.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    if (FxUtils.isESC(event)) {
                        if (!stageCloseService.isRunning()) {
                            stageCloseService.restart();
                        }
                    }
                }
            });
            stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                    new EventHandler<WindowEvent>() {

                        @Override
                        public void handle(WindowEvent event) {
                            logStage.close();
                        }
                    });
        }

        // init
        {
            settings.visibleSpectrums.set(true);
            settings.visibleSpectrums.set(false);
            settings.visibleVideoInfo.set(true);
            settings.visibleVideoInfo.set(false);
            settings.videoInfoSide.set(null);
            settings.videoInfoSide.set(InfoSide.RIGHT_BOTTOM);
            settings.timesVolumesHorizontal.set(true);
            settings.timesVolumesHorizontal.set(false);
            timeText.setText(LabelUtils.milliSecsToTime(0, 0, 0));
            volumeText.setText(LabelUtils.toVolume(Settings.DEFAULT_VOLUME));
        }

        log("Wake up !!");
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

    @FXML
    protected void controlPathText() {
        pathText.requestFocus();
    }

    @FXML
    protected void controlLog() {
        if (logStage.isShowing()) {
            logStage.hide();
        } else {
            logStage.show();
            logStage.toFront();
        }
    }

    @FXML
    protected void controlLogSnap() {
        logStage.setX(stage.getX());
        logStage.setY(stage.getY());
        if (logStage.isShowing()) {
            logStage.toFront();
        }
    }

    @FXML
    protected void controlPlayPause() {
        if (!controlPlayerForceWithSkip(Control.PLAY_PAUSE, null)) {
            syncControlPlayPause();
        }
    }

    protected void controlPlay() {
        if (!controlPlayerSoft(Control.PLAY, null)) {
            syncControlPlayPause();
        }
    }

    protected void controlPause() {
        if (!controlPlayerSoft(Control.PAUSE, null)) {
            syncControlPlayPause();
        }
    }

    @FXML
    protected void controlBack() {
        controlPlayerForceWithSkip(Control.BACK, null);
    }

    @FXML
    protected void controlNext() {
        controlPlayerForceWithSkip(Control.NEXT, null);
    }

    private enum Control {
        PLAY_PAUSE, PLAY, PAUSE, BACK, NEXT, SPECIFY;
    }

    private boolean controlPlayerSoft(Control control, MediaInfo info) {
        return controlPlayer(control, info, false, 0, false);
    }

    private boolean controlPlayerForceWithoutSkip(Control control, MediaInfo info) {
        return controlPlayer(control, info, true, 0, false);
    }

    private boolean controlPlayerForceWithSkip(Control control, MediaInfo info) {
        return controlPlayer(control, info, true, settings.skipOnError.get(), false);
    }

    private void controlPlayerLater(final Control control, final MediaInfo info,
            final boolean forceResolve, final int skipOnError, final boolean chain) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                controlPlayer(control, info, forceResolve, skipOnError, chain);
            }
        });
    }

    private boolean controlPlayer(final Control control, MediaInfo info,
            final boolean forceResolve, final int skipOnError, boolean chain) {

        // if too fast to be called repeatedly,
        //   returns false.
        boolean ret = true;

        player_block: {

            MediaInfo currentInfo = currentInfoProperty.getValue();
            MediaPlayer player = playerProperty.getValue();

            if (player != null) {
                boolean controlPP = control == Control.PLAY_PAUSE;
                boolean controlPl = control == Control.PLAY;
                boolean controlPs = control == Control.PAUSE;
                if (controlPP || controlPl || controlPs) {
                    Status status = player.getStatus();
                    if ((status == Status.PLAYING) && (controlPP || controlPs)) {

                        log("Control Pause : " + currentInfo.getDescription());

                        player.pause();

                        break player_block;
                    } else if ((status == Status.PAUSED) && (controlPP || controlPl)) {

                        requestScroll(currentInfo);

                        log("Control Resume : " + currentInfo.getDescription());

                        player.play();

                        break player_block;
                    } else if (controlPl || controlPs) {
                        ret = false;

                        break player_block;
                    }
                }
            }

            {
                // OMAJINAI!!!
                long currentTime = System.currentTimeMillis();
                if (!chain && (currentTime < (playerCreateTimeProperty.get() + 500))) {

                    ret = false;

                    break player_block;
                }
                playerCreateTimeProperty.set(currentTime);
            }

            // now loading ...
            if (player != null) {
                MediaException me = player.getError();
                boolean unknown = player.getStatus() == Status.UNKNOWN;
                if (unknown && (me == null)) {

                    log("Now Loading ... Please wait a minute.");

                    ret = false;

                    break player_block;
                }
            }

            final MediaInfo targetInfo;
            {
                List<MediaInfo> items = getItemsSnapshot();
                if (items.isEmpty()) {

                    syncControlPlayPause();

                    break player_block;
                }
                int index = -1;
                {
                    if (info == null) {
                        info = currentInfo;
                    }
                    if (info != null) {
                        index = items.indexOf(info);
                    }
                }
                if (index != -1) {
                    if (control == Control.NEXT) {
                        index++;
                        if (items.size() <= index) {
                            index = 0;
                        }
                    } else if (control == Control.BACK) {
                        index--;
                        if (index < 0) {
                            index = items.size() - 1;
                        }
                    }
                } else {
                    index = 0;
                }
                targetInfo = items.get(index);
            }

            log("Control Request : " + targetInfo.getDescription());

            final MediaPlayer mp;
            try {
                Media media = targetInfo.getMedia();
                if ((media == null) && forceResolve) {
                    media = targetInfo.forceResolveIfYet();
                    if (media == null) {
                        // wait for finishing future task...

                        // media = targetInfo.getMedia();

                        // by called targetInfo.resetIfYet(true),
                        //   next call targetInfo.getMedia() returns not null or throws MediaException.
                        //   as result, will not execute this block in next time.
                        controlPlayerLater(Control.SPECIFY, targetInfo, forceResolve,
                                skipOnError - 1, true);

                        break player_block;
                    }
                }
                if (media == null) {
                    throw new MediaLoadException("Media is null");
                }
                try {
                    mp = new MediaPlayer(media);
                } catch (MediaException e) {
                    // changed actual file URL ?

                    syncControlPlayPause();

                    onMediaPlayerError(e, targetInfo);

                    targetInfo.releaseMedia();
                    controlPlayerLater(Control.SPECIFY, targetInfo, forceResolve, skipOnError - 1,
                            true);

                    break player_block;
                }
            } catch (MediaLoadException e) {

                syncControlPlayPause();

                onMediaLoadError(e, targetInfo);

                if (0 < skipOnError) {
                    Boolean dir = directionProperty.getValue();
                    if ((dir == null) || dir.booleanValue()) {
                        controlPlayerLater(Control.NEXT, targetInfo, forceResolve, skipOnError - 1,
                                true);
                    } else {
                        controlPlayerLater(Control.BACK, targetInfo, forceResolve, skipOnError - 1,
                                true);
                    }
                }

                break player_block;
            }

            if (player != null) {

                if (player.getError() == null) {
                    currentInfo.mediaStatusProperty().setValue(MediaStatus.MEDIA_SUCCESS);
                }

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
                        if (targetInfo != currentInfoProperty.getValue()) {

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
                                                if (targetInfo == currentInfoProperty.getValue()) {
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
                            controlTime.setMax(v.doubleValue());
                        }
                        controlTime.maxProperty().bind(totalDuration);
                    }

                    // time current
                    {
                        mp.currentTimeProperty().addListener(new ChangeListener<Duration>() {

                            @Override
                            public void changed(ObservableValue<? extends Duration> observable,
                                    Duration oldValue, Duration newValue) {
                                if (targetInfo != currentInfoProperty.getValue()) {
                                    return;
                                }
                                if (controlTime.isValueChanging()) {
                                    return;
                                }
                                double newMillis = newValue.toMillis();
                                if (settings.thresholdTime(controlTime.getValue(), newMillis)) {
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
                                if (targetInfo != currentInfoProperty.getValue()) {
                                    return;
                                }
                                double newMillis = newValue.toMillis();
                                String newText = LabelUtils.milliSecsToTime(mp.getCurrentTime()
                                        .toMillis(), mp.getTotalDuration().toMillis(), newMillis);
                                if (!newText.equals(timeText.getText())) {
                                    timeText.setText(newText);
                                }
                                controlTimeBuffer.setProgress(newMillis
                                        / mp.getTotalDuration().toMillis());
                            }
                        });
                        controlTimeBuffer.setProgress(mp.getBufferProgressTime().toMillis()
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
                                if (targetInfo == currentInfoProperty.getValue()) {
                                    imageView.setImage(newValue);
                                    requestScroll(targetInfo);
                                }
                                targetInfo.imageProperty().removeListener(this);
                            }
                        });
                        imageView.setImage(targetInfo.imageProperty().getValue());
                        requestScroll(targetInfo);
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
                            MediaView old = videoPane.getMediaView();
                            if (old != null) {
                                destroyMediaView(old);
                            }
                            videoPane.setMediaView(mediaView);
                        }

                        videoTab.setText(LabelUtils.toTabLabel(targetInfo.getDescription()));
                    } else {
                        videoPane.getChildren().clear();

                        videoTab.setText(bundle.getString("tab.video"));
                    }

                    targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_READY);

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
                    imageView.setImage(new Image(PlayList.class.getResourceAsStream("dame.png")));

                    syncControlPlayPause();

                    onMediaPlayerError(mp.getError(), targetInfo);

                    if (0 < skipOnError) {
                        MediaPlayer player = playerProperty.getValue();
                        if ((player == null) || (mp == player)) {
                            Boolean dir = directionProperty.getValue();
                            if ((dir == null) || dir.booleanValue()) {
                                controlPlayer(Control.NEXT, targetInfo, forceResolve,
                                        skipOnError - 1, true);
                            } else {
                                controlPlayer(Control.BACK, targetInfo, forceResolve,
                                        skipOnError - 1, true);
                            }
                        }
                    }
                }
            });

            mp.setOnEndOfMedia(new Runnable() {

                @Override
                public void run() {

                    log("End of Media : " + targetInfo.getDescription());

                    targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_END);

                    {
                        Boolean dir = directionProperty.getValue();
                        if ((dir == null) || dir.booleanValue()) {
                            controlNext();
                        } else {
                            controlBack();
                        }
                    }
                }
            });

            mp.setOnPlaying(new Runnable() {

                @Override
                public void run() {
                    syncControlPlayPause();

                    targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_PLAYING);
                }
            });

            mp.setOnPaused(new Runnable() {

                @Override
                public void run() {
                    syncControlPlayPause();

                    targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_PAUSE);
                }
            });

            // cursor
            {
                if (currentInfo != null) {
                    currentInfo.cursorProperty().setValue(Boolean.FALSE);
                }
                targetInfo.cursorProperty().setValue(Boolean.TRUE);
            }

            // tab
            {
                if ((0 < targetInfo.getWidth()) || (0 < targetInfo.getHeight())) {
                    videoTab.setText(LabelUtils.toTabLabel(targetInfo.getDescription()));
                } else {
                    videoTab.setText(bundle.getString("tab.video"));
                }
            }

            targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_LOADING);

            currentInfoProperty.setValue(targetInfo);
            playerProperty.setValue(mp);
        }

        return ret;
    }

    private void onMediaLoadError(MediaLoadException ex, MediaInfo info) {
        log("Load Error : " + info.getDescription());
        if (ex != null) {
            log("Error Message : " + ex.getLocalizedMessage());
        }
        info.mediaStatusProperty().setValue(MediaStatus.MEDIA_ERROR);
    }

    private void onMediaPlayerError(MediaException ex, MediaInfo info) {
        log("Reading Error : " + info.getDescription());
        if (ex != null) {
            log("Error Message : " + ex.getLocalizedMessage());
        }
        info.mediaStatusProperty().setValue(MediaStatus.MEDIA_ERROR);
    }

    private void syncControlPlayPause() {
        MediaPlayer player = playerProperty.getValue();
        if ((player != null) && (player.getStatus() == Status.PLAYING)) {
            controlPlayPause.setSelected(true);
        } else {
            controlPlayPause.setSelected(false);
        }
    }

    private List<MediaInfo> getItemsSnapshot() {
        List<MediaInfo> ret;
        Lock lLock = infoListLock.readLock();
        try {
            lLock.lock();
            ret = new ArrayList<>(infoList.getItems());
        } finally {
            lLock.unlock();
        }
        return ret;
    }

    private void updateItems(String path, List<MediaInfo> items) {
        List<MediaInfo> dels = new ArrayList<>();
        Lock lock = infoListLock.writeLock();
        try {
            lock.lock();
            pathText.setText(path);
            {
                dels.addAll(infoList.getItems());
                infoList.getItems().clear();
                infoList.getItems().addAll(items);
            }
            {
                infoListClone.getItems().clear();
                infoListClone.getItems().addAll(items);
            }
        } finally {
            lock.unlock();
        }
        for (MediaInfo del : dels) {
            del.releaseMedia();
        }
    }

    private void requestScroll(final MediaInfo info) {
        if (info == null) {
            return;
        }
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                Lock lLock = infoListLock.readLock();
                try {
                    lLock.lock();
                    {
                        int idx = infoList.getItems().indexOf(info);
                        if (0 <= idx) {
                            infoList.getSelectionModel().select(idx);
                            infoList.scrollTo(idx);
                        }
                    }
                    {
                        int idx = infoListClone.getItems().indexOf(info);
                        if (0 <= idx) {
                            infoListClone.getSelectionModel().select(idx);
                            infoListClone.scrollTo(idx);
                        }
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
                    controlPlayPause();
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

    private static Tooltip installMediaInfoTooltip(Node node, final MediaInfo info,
            final double opacity) {
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
            ret.setOnShowing(new EventHandler<WindowEvent>() {

                @Override
                public void handle(WindowEvent event) {
                    if (!visible.getValue().booleanValue()) {
                        if (ret.getOpacity() != 0) {
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
                    } else if (ret.getOpacity() == 0) {
                        ret.setOpacity(opacity);
                    }
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
    // load media
    //-------------------------

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

    private final Service<Boolean> loadMediaService = new Service<Boolean>() {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    outer: for (int times = 2; 0 < times; times--) {
                        List<MediaInfo> items = getItemsSnapshot();
                        for (int i = 0; i < items.size(); i++) {
                            MediaInfo info = items.get(i);
                            if (info.loaded()) {
                                continue;
                            }
                            if (isCancelled()) {
                                break outer;
                            }
                            try {
                                if (info.load()) {
                                    if (isCancelled()) {
                                        break outer;
                                    }
                                    try {
                                        Thread.sleep(Math.max(
                                                settings.loadMediaStepMilliseconds.get(), 0));
                                        //Thread.yield();
                                    } catch (InterruptedException e) {
                                    }
                                    times++;
                                    break;
                                }
                            } catch (MediaLoadException e) {
                                onMediaLoadError(e, info);
                            }
                        }
                    }

                    return Boolean.TRUE;
                }
            };
        }
    };

    //----------------------------------------------------------------------------------
    // load list, save image services
    //-------------------------

    private final Service<List<MediaInfo>> loadDirectoryService;

    private final Service<List<MediaInfo>> loadFileService;

    private final Service<List<MediaInfo>> loadUrlService;

    private final Service<File> saveImageService;

    private final Service<Void> stageCloseService;

    {
        MetaChangeListener metaChangeListener = new MetaChangeListener() {

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
            }

            private boolean isOmit(String key) {
                return key.equals("raw metadata") || key.equals("image");
            }
        };
        loadDirectoryService = new LoadDirectoryService(stageProperty, metaChangeListener,
                new LoadDirectoryService.ICallback() {

                    @Override
                    public void callback(File dir, List<MediaInfo> infos) {
                        if (infos == null) {
                            log("Load Directory Failure : " + dir.getAbsolutePath());
                            return;
                        }
                        log("Load Directory : " + dir.getAbsolutePath());
                        updateItems(dir.toString(), infos);
                        loadMedia();
                    }
                });
        loadFileService = new LoadFileService(stageProperty, metaChangeListener,
                new LoadFileService.ICallback() {

                    @Override
                    public void callback(File file, List<MediaInfo> infos) {
                        if (infos == null) {
                            log("Load File Failure : " + file.getAbsolutePath());
                            return;
                        }
                        log("Load File : " + file.getAbsolutePath());
                        updateItems(file.toString(), infos);
                        loadMedia();
                    }
                });
        loadUrlService = new LoadUrlService(pathTextProperty, metaChangeListener,
                new LoadUrlService.ICallback() {

                    @Override
                    public void callback(String url, List<MediaInfo> infos) {
                        if (infos == null) {
                            log("Load URL Failure : " + url);
                            return;
                        }
                        log("Load URL : " + url);
                        updateItems(url, infos);
                        loadMedia();
                    }
                });
        saveImageService = new SaveImageService(currentInfoProperty, stageProperty,
                new SaveImageService.ICallback() {

                    @Override
                    public void callback(File in, File out, MediaInfo info) {
                        if (out == null) {
                            log("Save Image Failure : " + in.getAbsolutePath());
                            return;
                        }
                        log("Save Image : " + out.getAbsolutePath());
                    }
                });
        stageCloseService = new StageCloseService(stageProperty);
    }

    private boolean loadRunning() {
        return loadDirectoryService.isRunning() || loadFileService.isRunning()
                || loadUrlService.isRunning();
    }

    //----------------------------------------------------------------------------------
    // shortcuts
    //-------------------------

    @FXML
    protected void keyTypedHandle(KeyEvent event) {
        shortCutHandler.handle(event);
    }

    private final EventHandler<KeyEvent> shortCutHandler = new ShortcutHandler(
            new ShortcutHandler.IAdapter() {

                @Override
                public void controlDirectoryChooser() {
                    PlayList.this.controlDirectoryChooser();
                }

                @Override
                public void controlFileChooser() {
                    PlayList.this.controlFileChooser();
                }

                @Override
                public void focusPathText() {
                    PlayList.this.controlPathText();
                }

                @Override
                public void controlLog() {
                    PlayList.this.controlLog();
                }

                @Override
                public void controlLogSnap() {
                    PlayList.this.controlLogSnap();
                }

                @Override
                public void controlLogFront() {
                    logStage.toFront();
                }

                @Override
                public void controlLogBack() {
                    stage.toFront();
                }

                @Override
                public void controlLogAuto() {
                    // TODO
                    // check auto front in log window
                    // no implement now
                }

                @Override
                public void controlTab() {
                    SingleSelectionModel<Tab> selection = videoTab.getTabPane().getSelectionModel();
                    if (selection.isEmpty()) {
                        return;
                    }
                    int ci = selection.getSelectedIndex();
                    selection.selectNext();
                    if ((ci != 0) && (ci == selection.getSelectedIndex())) {
                        selection.selectFirst();
                    }
                }

                @Override
                public void controlTabSide() {
                    settings.toggleTabSide();
                }

                @Override
                public void controlVideoInfoSide() {
                    settings.toggleVisibleVideoInfoSide();
                }

                @Override
                public void controlSpectrums() {
                    settings.toggleVisibleSpectrums();
                }

                @Override
                public void controlTimesVolumes() {
                    settings.toggleTimesVolumesHorizontal();
                }

                @Override
                public void controlPlayPause() {
                    PlayList.this.controlPlayPause();
                }

                @Override
                public void controlPlay() {
                    PlayList.this.controlPlay();
                }

                @Override
                public void controlPause() {
                    PlayList.this.controlPause();
                }

                @Override
                public void controlBack() {
                    PlayList.this.controlBack();
                }

                @Override
                public void controlNext() {
                    PlayList.this.controlNext();
                }

                @Override
                public void controlTime(long seconds) {
                    double value = seconds * 1000;
                    controlTime.setValue(Math.max(controlTime.getMin(),
                            Math.min(value, controlTime.getMax() - 1)));
                }

                @Override
                public void controlTimePlus(long seconds) {
                    double value = controlTime.getValue() + (seconds * 1000);
                    controlTime.setValue(Math.max(controlTime.getMin(),
                            Math.min(value, controlTime.getMax() - 1)));
                }

                @Override
                public void controlTimeMinus(long seconds) {
                    controlTimePlus(seconds * -1);
                }

                @Override
                public void controlVolume(int volume) {
                    double value = volume / 100d;
                    controlVolume.setValue(Math.max(controlVolume.getMin(),
                            Math.min(value, controlVolume.getMax())));
                }

                @Override
                public void controlVolumePlus(int volume) {
                    double value = controlVolume.getValue() + (volume / 100d);
                    controlVolume.setValue(Math.max(controlVolume.getMin(),
                            Math.min(value, controlVolume.getMax())));
                }

                @Override
                public void controlVolumeMinus(int volume) {
                    controlVolumePlus(volume * -1);
                }

                @Override
                public void controlJump(int value) {
                    List<MediaInfo> infos = getItemsSnapshot();
                    int size = infos.size();
                    if (size == 0) {
                        return;
                    }
                    int idx = Math.min(Math.max(value, 1), size) - 1;
                    controlPlayerForceWithoutSkip(Control.SPECIFY, infos.get(idx));
                }

                @Override
                public void controlJumpPlus(int value) {
                    List<MediaInfo> infos = getItemsSnapshot();
                    int size = infos.size();
                    if (size == 0) {
                        return;
                    }
                    MediaInfo currentInfo = currentInfoProperty.getValue();
                    int currentIdx = (currentInfo == null) ? -1 : infos.indexOf(currentInfo);
                    int idx = Math.max(currentIdx, 0) + value;
                    idx = idx % size;
                    while (idx < 0) {
                        idx += size;
                    }
                    controlPlayerForceWithoutSkip(Control.SPECIFY, infos.get(idx));
                }

                @Override
                public void controlJumpMinus(int value) {
                    controlJumpPlus(value * -1);
                }

                @Override
                public void controlSaveFile() {
                    // TODO
                    // save list file
                    // no implement now
                }

                @Override
                public void controlSaveImage() {
                    if (!saveImageService.isRunning()) {
                        saveImageService.restart();
                    }
                }

                @Override
                public void controlWindowScreen() {
                    stage.setFullScreen(!stage.isFullScreen());
                }

                @Override
                public void controlWindowScreenMin() {
                    if (stage.isFullScreen()) {
                        stage.setFullScreen(false);
                    }
                }

                @Override
                public void controlWindowScreenMax() {
                    if (!stage.isFullScreen()) {
                        stage.setFullScreen(true);
                    }
                }

                @Override
                public void controlWindowFront() {
                    stage.toFront();
                }

                @Override
                public void controlWindowBack() {
                    logStage.toFront();
                }

                @Override
                public void controlWindowIconified() {
                    if (stage.isShowing()) {
                        stage.setIconified(true);
                    }
                    if (logStage.isShowing()) {
                        logStage.setIconified(true);
                    }
                }

                @Override
                public void controlWindowExit() {
                    if (!stageCloseService.isRunning()) {
                        stageCloseService.restart();
                    }
                }
            });

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
        logStage.appendLog(String.format("%1$tH:%1$tM:%1$tS:%1$tL - %2$s%n", new Date(), str));
    }
}
