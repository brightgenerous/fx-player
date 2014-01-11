package com.brightgenerous.fxplayer.application.playlist;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.Node;
import javafx.scene.SnapshotResult;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
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
import com.brightgenerous.fxplayer.service.LoadDirection;
import com.brightgenerous.fxplayer.service.LoadDirectoryService;
import com.brightgenerous.fxplayer.service.LoadFilesService;
import com.brightgenerous.fxplayer.service.LoadListFileService;
import com.brightgenerous.fxplayer.service.LoadUrlService;
import com.brightgenerous.fxplayer.service.SaveImageService;
import com.brightgenerous.fxplayer.service.SaveImageService.ImageInfo;
import com.brightgenerous.fxplayer.service.StageCloseService;
import com.brightgenerous.fxplayer.url.UrlDispathcer;
import com.brightgenerous.fxplayer.util.ListUtils;

public class PlayList implements Initializable {

    private final Settings settings = new Settings();

    @Inject
    private Stage stage;

    private final ObjectProperty<Stage> stageProperty = new SimpleObjectProperty<>();

    private ResourceBundle bundle;

    // pane

    @FXML
    private StoreVBox rootPane;

    @FXML
    private ToggleButton controlHideHeader;

    @FXML
    private ToggleButton controlHideFooter;

    // top - control

    @FXML
    private TextField pathText;

    private final Property<String> pathTextProperty = new SimpleStringProperty();

    @FXML
    private ToggleButton controlLog;

    private LogStage logStage;

    // tab

    @FXML
    private TabWrapPane tabWrapPane;

    // tab - info

    @FXML
    private Tab infoTab;

    @FXML
    private TableView<MediaInfo> infoList;

    // tab - video

    @FXML
    private Tab videoTab;

    private NumberBinding mediaViewFitWidth;

    private NumberBinding mediaViewFitHeight;

    @FXML
    private VideoPane videoPane;

    @FXML
    private TableView<MediaInfo> infoListClone;

    // bottom - control

    @FXML
    private GridPane timesVolumesPane;

    @FXML
    private Pane timesPane;

    @FXML
    private Pane volumesPane;

    // bottom - control - play

    @FXML
    private ToggleButton controlPlayPause;

    @FXML
    private Label repeatText;

    @FXML
    private Label directionText;

    // bottom - control - time

    @FXML
    private BufferSlider bufferSlider; // 0.0 - ...  milliseconds

    @FXML
    private Label timeText;

    // control - volume

    @FXML
    private Slider controlVolume; // 0.0 - 1.0

    @FXML
    private Label muteText;

    @FXML
    private Label volumeText;

    // image

    @FXML
    private ImageView imageView;

    // something

    @FXML
    private Pane spectrumsWrap;

    @FXML
    private Spectrums spectrums;

    // other

    @FXML
    private Node bottomSpace;

    // state properties

    private final ObjectProperty<MediaInfo> currentInfoProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<MediaPlayer> playerProperty = new SimpleObjectProperty<>();

    private final LongProperty playerCreateTimeProperty = new SimpleLongProperty(Long.MIN_VALUE);

    private final ReadWriteLock infoListLock = new ReentrantReadWriteLock();

    private boolean logging;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        {
            bundle = resources;
            stageProperty.setValue(stage);
            pathTextProperty.bind(pathText.textProperty());
        }

        // screen
        {
            final BooleanProperty mouseOnHeader = new SimpleBooleanProperty(false);
            final BooleanProperty mouseOnFooter = new SimpleBooleanProperty(false);
            rootPane.hideHeaderProperty().bind(
                    Bindings.when(settings.hideHeader).then(mouseOnHeader.not()).otherwise(false));
            rootPane.hideFooterProperty().bind(
                    Bindings.when(settings.hideFooter).then(mouseOnFooter.not()).otherwise(false));

            controlHideHeader.textProperty().bind(
                    Bindings.when(controlHideHeader.selectedProperty())
                            .then(bundle.getString("control.hideHeader.on"))
                            .otherwise(bundle.getString("control.hideHeader.off")));
            controlHideHeader.ellipsisStringProperty().bind(
                    Bindings.when(controlHideHeader.selectedProperty())
                            .then(bundle.getString("control.hideHeader.ellipsis.on"))
                            .otherwise(bundle.getString("control.hideHeader.ellipsis.off")));
            settings.hideHeader.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    controlHideHeader.setSelected(!newValue.booleanValue());
                }
            });
            controlHideFooter.textProperty().bind(
                    Bindings.when(controlHideFooter.selectedProperty())
                            .then(bundle.getString("control.hideFooter.on"))
                            .otherwise(bundle.getString("control.hideFooter.off")));
            controlHideFooter.ellipsisStringProperty().bind(
                    Bindings.when(controlHideFooter.selectedProperty())
                            .then(bundle.getString("control.hideFooter.ellipsis.on"))
                            .otherwise(bundle.getString("control.hideFooter.ellipsis.off")));
            settings.hideFooter.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    controlHideFooter.setSelected(!newValue.booleanValue());
                }
            });

            rootPane.addEventFilter(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    double y = event.getSceneY();
                    if (y < Math.max(rootPane.getHeaderHeight(), 20)) {
                        mouseOnHeader.set(true);
                    } else {
                        mouseOnHeader.set(false);
                    }
                    double height = rootPane.getHeight();
                    if (Double.isNaN(height) || (height < 0)) {
                        return;
                    }
                    if ((height - y) <= Math.max(rootPane.getFooterHeight(), 20)) {
                        mouseOnFooter.set(true);
                    } else {
                        mouseOnFooter.set(false);
                    }
                }
            });
            rootPane.getHeader().addEventHandler(MouseEvent.MOUSE_EXITED,
                    new EventHandler<MouseEvent>() {

                        @Override
                        public void handle(MouseEvent event) {
                            mouseOnHeader.set(false);
                        }
                    });
            rootPane.getFooter().addEventHandler(MouseEvent.MOUSE_EXITED,
                    new EventHandler<MouseEvent>() {

                        @Override
                        public void handle(MouseEvent event) {
                            mouseOnFooter.set(false);
                        }
                    });
        }

        // tab
        {
            videoTab.getTabPane().tabMinHeightProperty().bind(settings.tabHeight);
            videoTab.getTabPane().tabMaxHeightProperty().bind(settings.tabHeight);

            tabWrapPane.tabHeightProperty().bind(settings.tabSpaceHeight);
            tabWrapPane.visibleTabProperty().bind(settings.visibleTab);

            videoTab.getTabPane().sideProperty().bind(settings.tabSide);
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
        }

        // video
        {
            settings.visibleVideoInfo.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    videoPane.setVisibleInfoList(newValue.booleanValue());
                }
            });

            settings.videoInfoSide.addListener(new ChangeListener<InfoSide>() {

                @Override
                public void changed(ObservableValue<? extends InfoSide> observable,
                        InfoSide oldValue, InfoSide newValue) {
                    videoPane.setInfoSide(newValue);
                }
            });

            videoPane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        controlSaveSnapshot();
                    }
                }
            });
            {
                BooleanBinding infoHashSize = settings.visibleVideoInfo.and(settings.videoInfoSide
                        .isNotEqualTo(InfoSide.OVERLAY));
                NumberBinding videoInfoMinWidth = Bindings
                        .when(infoHashSize)
                        .then(Bindings.min(settings.videoInfoMinWidth, videoTab.getTabPane()
                                .widthProperty().divide(2))).otherwise(0);
                NumberBinding videoInfoMinHeight = Bindings
                        .when(infoHashSize)
                        .then(Bindings.min(settings.videoInfoMinHeight, videoTab.getTabPane()
                                .heightProperty().divide(2))).otherwise(0);
                mediaViewFitWidth = videoTab.getTabPane().widthProperty()
                        .subtract(settings.tabMarginWidth).subtract(videoInfoMinWidth);
                mediaViewFitHeight = videoTab.getTabPane().heightProperty()
                        .subtract(settings.tabMarginHeight).subtract(videoInfoMinHeight);

                videoPane.videoInfoMaxWidthProperty().bind(settings.videoInfoMaxWidth);
                videoPane.videoInfoMaxHeightProperty().bind(settings.videoInfoMaxHeight);
            }

            timeText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        settings.toggleVislbleTabSide();
                    }
                }
            });
        }

        // info list
        {
            infoList.setRowFactory(new Callback<TableView<MediaInfo>, TableRow<MediaInfo>>() {

                private final EventHandler<MouseEvent> clickListener = new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {
                        if (event.getClickCount() == 2) {
                            Object source = event.getSource();
                            if (source instanceof TableRow) {
                                TableRow<?> row = (TableRow<?>) source;
                                Object obj = row.getItem();
                                if (obj instanceof MediaInfo) {
                                    controlPlayerForceWithSkip(Control.SPECIFY, (MediaInfo) obj);
                                }
                            }
                        }
                    }
                };

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

            // clone
            infoListClone.setRowFactory(infoList.getRowFactory());
            infoListClone.opacityProperty().bind(
                    Bindings.when(settings.videoInfoSide.isEqualTo(InfoSide.OVERLAY)).then(0.3)
                            .otherwise(0.7));

            // drag on drop
            infoList.setOnDragOver(new EventHandler<DragEvent>() {

                @Override
                public void handle(DragEvent event) {
                    event.acceptTransferModes(TransferMode.LINK);
                }
            });
            infoList.setOnDragDropped(new EventHandler<DragEvent>() {

                @Override
                public void handle(DragEvent event) {
                    Dragboard dragboard = event.getDragboard();
                    if (dragboard != null) {
                        controlAppendFiles(dragboard.getFiles());
                    }
                }
            });
            infoListClone.setOnDragOver(infoList.getOnDragOver());
            infoListClone.setOnDragDropped(infoList.getOnDragDropped());
        }

        // control play
        {
            settings.nextMode.addListener(new ChangeListener<NextMode>() {

                @Override
                public void changed(ObservableValue<? extends NextMode> observable,
                        NextMode oldValue, NextMode newValue) {
                    switch (newValue) {
                        case NONE:
                            repeatText.setText(bundle.getString("control.repeat.none"));
                            break;
                        case SAME:
                            repeatText.setText(bundle.getString("control.repeat.same"));
                            break;
                        case OTHER:
                            repeatText.setText(bundle.getString("control.repeat.other"));
                            break;
                    }
                }
            });
            repeatText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        settings.toggleNextMode();
                    }
                }
            });

            settings.otherDirection.addListener(new ChangeListener<OtherDirection>() {

                @Override
                public void changed(ObservableValue<? extends OtherDirection> observable,
                        OtherDirection oldValue, OtherDirection newValue) {
                    switch (newValue) {
                        case FORWARD:
                            directionText.setText(bundle.getString("control.direction.forward"));
                            break;
                        case BACK:
                            directionText.setText(bundle.getString("control.direction.back"));
                            break;
                    }
                }
            });
            directionText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        settings.toggleOtherDirection();
                    }
                }
            });
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
            final Slider controlTime = bufferSlider.getSlider();
            controlTime.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                        Number newValue) {
                    double newMillis = newValue.doubleValue();
                    double oldMillis = oldValue.doubleValue();
                    if (!controlTime.isValueChanging() && (Math.abs(newMillis - oldMillis) < 5_000)) {
                        // fired event does not start from this control
                        //   chained event is.
                        // but...
                        //   when click this control, "isValueChanging()" returns "false"
                        //   so, if the difference is greater 5000, to be considered to have been clicked.
                        return;
                    }
                    MediaPlayer player = playerProperty.getValue();
                    if (player == null) {
                        return;
                    }
                    Status status = player.getStatus();
                    if ((status == Status.UNKNOWN) || (status == Status.DISPOSED)) {
                        return;
                    }
                    double curMillis = player.getCurrentTime().toMillis();
                    if (settings.thresholdTimeSeek(curMillis, newMillis)) {
                        if (newMillis < player.getBufferProgressTime().toMillis()) {
                            player.seek(Duration.millis(newMillis));

                            String oldTime = LabelUtils.milliSecToTime(curMillis);
                            String newTime = LabelUtils.milliSecToTime(newMillis);
                            if (!oldTime.equals(newTime)) {
                                log("Control Time : old => " + oldTime + " , new => " + newTime);
                            }
                        }
                    }
                }
            });

            controlTime.setBlockIncrement(10_000);
        }

        // control volume
        {
            controlVolume.valueProperty().bindBidirectional(settings.volume);
            settings.volume.addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                        Number newValue) {
                    player: {
                        MediaPlayer player = playerProperty.getValue();
                        if (player == null) {
                            break player;
                        }
                        Status status = player.getStatus();
                        if ((status == Status.UNKNOWN) || (status == Status.DISPOSED)) {
                            break player;
                        }
                        double oldVol = player.getVolume();
                        double newVol = newValue.doubleValue();
                        if (0.01 <= Math.abs(oldVol - newVol)) {
                            player.setVolume(newVol);
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

            settings.mute.addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    MediaPlayer player = playerProperty.getValue();
                    if (newValue.booleanValue()) {
                        if ((player != null) && !player.isMute()) {
                            player.setMute(true);
                        }

                        muteText.setText(bundle.getString("control.mute.on"));

                        log("Control Volume : off ");
                    } else {
                        if ((player != null) && player.isMute()) {
                            player.setMute(false);
                        }

                        muteText.setText(bundle.getString("control.mute.off"));

                        log("Control Volume : on ");
                    }
                }
            });
            muteText.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        settings.toggleMute();
                    }
                }
            });
        }

        // image
        {
            imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        controlSaveImage();
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

        // other
        {
            bottomSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    int count = event.getClickCount();
                    if ((1 < count) && ((count % 2) == 0)) {
                        settings.toggleTimesVolumesHorizontal();
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
            logStage = new LogStage(stageProperty, shortCutHandler);
            logStage.setOnHidden(new EventHandler<WindowEvent>() {

                @Override
                public void handle(WindowEvent event) {
                    controlLog.setSelected(false);
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
            settings.reset();
            timeText.setText(LabelUtils.milliSecsToTime(0, 0, 0));
        }

        logging = true;
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
            loadListFileService.restart();
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
    protected void controlHideHeader() {
        settings.toggleHideHeader();
    }

    @FXML
    protected void controlHideFooter() {
        settings.toggleHideFooter();
    }

    @FXML
    protected void controlPlayPause() {
        controlPlayerForceWithSkip(Control.PLAY_PAUSE, null);
    }

    protected void controlPlay() {
        controlPlayerSoft(Control.PLAY, null);
    }

    protected void controlPause() {
        controlPlayerSoft(Control.PAUSE, null);
    }

    @FXML
    protected void controlHead() {
        MediaPlayer player = playerProperty.getValue();
        if (player == null) {
            return;
        }
        Status status = player.getStatus();
        if ((status != Status.UNKNOWN) && (status != Status.DISPOSED)) {
            player.seek(Duration.millis(0));
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
        return controlPlayer(control, info, false, 0, false, control);
    }

    private boolean controlPlayerForceWithSkip(Control control, MediaInfo info) {
        return controlPlayer(control, info, true, settings.skipOnError.get(), false, control);
    }

    private void controlPlayerLater(Control control, MediaInfo info, boolean forceResolve,
            int skipOnError, boolean chain) {
        controlPlayerLater(control, info, forceResolve, skipOnError, chain, control);
    }

    private void controlPlayerLater(final Control control, final MediaInfo info,
            final boolean forceResolve, final int skipOnError, final boolean chain,
            final Control trigger) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                controlPlayer(control, info, forceResolve, skipOnError, chain, trigger);
            }
        });
    }

    private boolean controlPlayer(final Control control, MediaInfo info,
            final boolean forceResolve, final int skipOnError, boolean chain, final Control trigger) {

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
                if ((player.getError() == null) && (player.getStatus() == Status.UNKNOWN)) {

                    log("Now Loading ... Please wait a minute.");

                    ret = false;

                    break player_block;
                }
            }

            final MediaInfo targetInfo;
            {
                List<MediaInfo> items = getItemsSnapshot();
                if (items.isEmpty()) {
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
                            if (onNextFromTail()) {
                                break player_block;
                            }
                            index = 0;
                        }
                    } else if (control == Control.BACK) {
                        index--;
                        if (index < 0) {
                            if (onBackFromHead()) {
                                break player_block;
                            }
                            index = items.size() - 1;
                        }
                    }
                } else {
                    if (control == Control.BACK) {
                        index = items.size() - 1;
                    } else {
                        index = 0;
                    }
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
                        controlPlayerLater(Control.SPECIFY, targetInfo, forceResolve, skipOnError,
                                true, trigger);

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
                    onMediaPlayerError(e, targetInfo);

                    targetInfo.releaseMedia(true);
                    if (0 < skipOnError) {
                        if (trigger != Control.SPECIFY) {
                            OtherDirection otherDirection = settings.otherDirection.getValue();
                            if (otherDirection == null) {
                                otherDirection = OtherDirection.FORWARD;
                            }
                            switch (otherDirection) {
                                case FORWARD:
                                    controlPlayerLater(Control.NEXT, targetInfo, forceResolve,
                                            skipOnError - 1, true, trigger);
                                    break;
                                case BACK:
                                    controlPlayerLater(Control.BACK, targetInfo, forceResolve,
                                            skipOnError - 1, true, trigger);
                                    break;
                            }
                        }
                    }

                    break player_block;
                }
            } catch (MediaLoadException e) {
                onMediaLoadError(e, targetInfo);

                if (0 < skipOnError) {
                    if (trigger != Control.SPECIFY) {
                        OtherDirection otherDirection = settings.otherDirection.getValue();
                        if (otherDirection == null) {
                            otherDirection = OtherDirection.FORWARD;
                        }
                        switch (otherDirection) {
                            case FORWARD:
                                controlPlayerLater(Control.NEXT, targetInfo, forceResolve,
                                        skipOnError - 1, true, trigger);
                                break;
                            case BACK:
                                controlPlayerLater(Control.BACK, targetInfo, forceResolve,
                                        skipOnError - 1, true, trigger);
                                break;
                        }
                    }
                }

                break player_block;
            }

            if (player != null) {

                if (player.getError() == null) {
                    currentInfo.mediaStatusProperty().setValue(MediaStatus.MEDIA_SUCCESS);
                }

                if (player.getStatus() != Status.DISPOSED) {
                    player.dispose();
                }
            }

            mp.setOnReady(new Runnable() {

                @Override
                public void run() {
                    Duration dur = mp.getTotalDuration();
                    if ((dur != null) && !dur.isUnknown()) {
                        targetInfo.durationProperty().setValue(dur);
                    }

                    // this callback would run on main thread.
                    //   so here, (targetInfo == current) is Deterministic ?
                    if (!checkInfoOnEvent(targetInfo, mp)) {
                        return;
                    }

                    final Slider controlTime = bufferSlider.getSlider();

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
                                targetInfo
                                        .replaceDurationChangeListener(new ChangeListener<Duration>() {

                                            @Override
                                            public void changed(
                                                    ObservableValue<? extends Duration> observable,
                                                    Duration oldValue, Duration newValue) {
                                                if (equalsCurrentInfo(targetInfo)) {
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
                                if (!equalsCurrentInfo(targetInfo)) {
                                    return;
                                }
                                if (controlTime.isValueChanging()) {
                                    // fired event does not start from currentTimeProperty.changed
                                    //   chained event is.
                                    return;
                                }
                                double newMillis = newValue.toMillis();
                                if (settings.thresholdTimeCurrent(controlTime.getValue(), newMillis)) {
                                    controlTime.setValue(newMillis);

                                    String newText = LabelUtils.milliSecsToTime(newMillis, mp
                                            .getTotalDuration().toMillis(), mp
                                            .getBufferProgressTime().toMillis());
                                    if (!newText.equals(timeText.getText())) {
                                        timeText.setText(newText);
                                    }
                                }
                            }
                        });
                    }

                    // time buffer
                    {
                        ReadOnlyObjectProperty<Duration> bptp = mp.bufferProgressTimeProperty();
                        bptp.addListener(new ChangeListener<Duration>() {

                            @Override
                            public void changed(ObservableValue<? extends Duration> observable,
                                    Duration oldValue, Duration newValue) {
                                if (!equalsCurrentInfo(targetInfo)) {
                                    return;
                                }
                                double newMillis = newValue.toMillis();
                                String newText = LabelUtils.milliSecsToTime(mp.getCurrentTime()
                                        .toMillis(), mp.getTotalDuration().toMillis(), newMillis);
                                if (!newText.equals(timeText.getText())) {
                                    timeText.setText(newText);
                                }
                                bufferSlider.setBuffer(newMillis);
                            }
                        });
                        Duration bpt = bptp.getValue();
                        if (bpt != null) {
                            bufferSlider.setBuffer(bpt.toMillis());
                        }
                    }

                    // volume
                    {
                        mp.setVolume(settings.volume.get());
                    }

                    // image
                    {
                        targetInfo.replaceImageChangeListener(new ChangeListener<Image>() {

                            @Override
                            public void changed(ObservableValue<? extends Image> observable,
                                    Image oldValue, Image newValue) {
                                if (equalsCurrentInfo(targetInfo)) {
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
                                spectrums.spectrumDataUpdate(timestamp, duration, magnitudes,
                                        phases);
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
                        videoPane.setMediaView(null);

                        videoTab.setText(bundle.getString("tab.video"));
                    }

                    targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_READY);

                    // play
                    {
                        mp.setMute(settings.mute.get());

                        mp.play();
                        syncControlPlayPauseLater();

                        log("Control Play : " + targetInfo.getDescription());
                    }
                }
            });

            mp.setOnError(new Runnable() {

                @Override
                public void run() {
                    if (!checkInfoOnEvent(targetInfo, mp)) {
                        return;
                    }

                    imageView.setImage(new Image(PlayList.class.getResourceAsStream("dame.png")));

                    syncControlPlayPause();

                    onMediaPlayerError(mp.getError(), targetInfo);

                    targetInfo.releaseMedia(true);
                    if (0 < skipOnError) {
                        MediaPlayer player = playerProperty.getValue();
                        if ((player == null) || (mp == player)) {
                            if (targetInfo.getLast()) {
                                targetInfo.setLast(false);
                                controlPlayer(Control.SPECIFY, targetInfo, forceResolve,
                                        skipOnError - 1, true, trigger);
                            }
                            //
                            // Duration dur = mp.getCurrentTime();
                            // when dur.equals(Duration.ZERO) then it will be such as swf.
                            //  so, should be skipped.
                            //
                            OtherDirection otherDirection = settings.otherDirection.getValue();
                            if (otherDirection == null) {
                                otherDirection = OtherDirection.FORWARD;
                            }
                            switch (otherDirection) {
                                case FORWARD:
                                    controlPlayer(Control.NEXT, targetInfo, forceResolve,
                                            skipOnError - 1, true, trigger);
                                    break;
                                case BACK:
                                    controlPlayer(Control.BACK, targetInfo, forceResolve,
                                            skipOnError - 1, true, trigger);
                                    break;
                            }
                        }
                    }
                }
            });

            mp.setOnEndOfMedia(new Runnable() {

                @Override
                public void run() {
                    if (!checkInfoOnEvent(targetInfo, mp)) {
                        return;
                    }

                    log("End of Media : " + targetInfo.getDescription());

                    mp.dispose();
                    syncControlPlayPauseLater();

                    targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_END);

                    {
                        NextMode nextMode = settings.nextMode.getValue();
                        if (nextMode == null) {
                            nextMode = NextMode.NONE;
                        }
                        switch (nextMode) {
                            case NONE:
                                break;
                            case SAME:
                                controlPlayer(Control.SPECIFY, targetInfo, true,
                                        settings.skipOnError.get(), true, trigger);
                                break;
                            case OTHER:
                                OtherDirection otherDirection = settings.otherDirection.getValue();
                                if (otherDirection == null) {
                                    otherDirection = OtherDirection.FORWARD;
                                }
                                switch (otherDirection) {
                                    case FORWARD:
                                        controlPlayer(Control.NEXT, targetInfo, true,
                                                settings.skipOnError.get(), true, trigger);
                                        break;
                                    case BACK:
                                        controlPlayer(Control.BACK, targetInfo, true,
                                                settings.skipOnError.get(), true, trigger);
                                        break;
                                }
                                break;
                        }
                    }
                }
            });

            mp.setOnPlaying(new Runnable() {

                @Override
                public void run() {
                    if (!checkInfoOnEvent(targetInfo, mp)) {
                        return;
                    }

                    syncControlPlayPause();

                    targetInfo.setLast(true);

                    targetInfo.mediaStatusProperty().setValue(MediaStatus.PLAYER_PLAYING);
                }
            });

            mp.setOnPaused(new Runnable() {

                @Override
                public void run() {
                    if (!checkInfoOnEvent(targetInfo, mp)) {
                        return;
                    }

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

        syncControlPlayPause();

        return ret;
    }

    private boolean equalsCurrentInfo(MediaInfo info) {
        return info == currentInfoProperty.getValue();
    }

    private boolean checkInfoOnEvent(MediaInfo info, MediaPlayer mp) {
        if (info != currentInfoProperty.getValue()) {

            log("WARNING!!! : targetInfo != current");

            if ((mp != null) && (mp.getStatus() != Status.DISPOSED)) {
                mp.dispose();
            }
            return false;
        }
        return true;
    }

    private boolean onNextFromTail() {
        return movePageIfEnable(1, true);
    }

    private boolean onBackFromHead() {
        return movePageIfEnable(-1, true);
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

    private void syncControlPlayPauseLater() {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                syncControlPlayPause();
            }
        });
    }

    private void syncControlPlayPause() {
        MediaPlayer player = playerProperty.getValue();
        if (player == null) {
            if (controlPlayPause.isSelected()) {
                controlPlayPause.setSelected(false);
            }
        } else {
            if (player.getStatus() == Status.PLAYING) {
                double ct = player.getCurrentTime().toMillis();
                double mt = player.getTotalDuration().toMillis();
                if (ct < mt) {
                    if (!controlPlayPause.isSelected()) {
                        controlPlayPause.setSelected(true);
                    }
                } else {
                    if (controlPlayPause.isSelected()) {
                        controlPlayPause.setSelected(false);
                    }
                }
            } else {
                if (controlPlayPause.isSelected()) {
                    controlPlayPause.setSelected(false);
                }
            }
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

    private void updateItems(String path, List<? extends MediaInfo> items) {
        updateItems(path, items, false);
    }

    private void appendItems(List<? extends MediaInfo> items) {
        updateItems(null, items, true);
    }

    private void updateItems(String path, List<? extends MediaInfo> items, boolean append) {
        List<MediaInfo> dels = new ArrayList<>();
        Lock lock = infoListLock.writeLock();
        try {
            lock.lock();
            if (path != null) {
                pathText.setText(path);
            }
            {
                if (!append) {
                    ObservableList<MediaInfo> is = infoList.getItems();
                    dels.addAll(is);
                    is.setAll(items);
                } else {
                    infoList.getItems().addAll(items);
                }
            }
            {
                if (!append) {
                    infoListClone.getItems().setAll(items);
                } else {
                    infoListClone.getItems().addAll(items);
                }
            }
        } finally {
            lock.unlock();
        }
        for (MediaInfo del : dels) {
            del.releaseMedia(false);
        }
    }

    private void removeItems(Collection<MediaInfo> infos) {
        if ((infos == null) || infos.isEmpty()) {
            return;
        }
        Lock lock = infoListLock.writeLock();
        try {
            lock.lock();
            infoList.getItems().removeAll(infos);
            infoListClone.getItems().removeAll(infos);
        } finally {
            lock.unlock();
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
        mediaView.fitWidthProperty().bind(mediaViewFitWidth);
        mediaView.fitHeightProperty().bind(mediaViewFitHeight);
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
                            imageView.setFitHeight(80);
                        }
                    }
                });
                imageView.imageProperty().bind(info.imageProperty());
            }
            ret.setGraphic(imageView);
            ret.setId("media-info-tooltip");
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
                            info.replaceVisibleTooltipChangeListener(new ChangeListener<Boolean>() {

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
                        if (isCancelled()) {
                            break outer;
                        }

                        List<MediaInfo> items = getItemsSnapshot();
                        LoadDirection loadDirection = settings.loadDirection.getValue();
                        if (loadDirection == null) {
                            loadDirection = LoadDirection.ALTERNATELY;
                        }
                        switch (loadDirection) {
                            case FORWARD:
                                break;
                            case BACK:
                                items = ListUtils.toReverse(items);
                                break;
                            case ALTERNATELY:
                                items = ListUtils.toAlternate(items);
                                break;
                        }
                        for (int i = 0; i < items.size(); i++) {
                            MediaInfo info = items.get(i);
                            if (!info.enablePreLoad() || info.loaded()) {
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
    // other controls
    // ------------------------

    private void controlRemove(MediaInfo info) {
        if (info != null) {
            removeItems(Arrays.asList(info));
        }
    }

    private void controlAppendFiles(List<? extends File> files) {
        if ((files == null) || files.isEmpty()) {
            return;
        }
        if (!loadFilesService.isRunning()) {
            filesProperty.setValue(files);
            loadFilesService.restart();
        }
    }

    private void controlSaveImage() {
        if (!saveTagImageService.isRunning()) {
            saveTagImageService.restart();
        }
    }

    private void controlSaveSnapshot() {
        MediaView mediaView = videoPane.getMediaView();
        if (mediaView == null) {
            return;
        }
        MediaInfo mediaInfo = currentInfoProperty.getValue();
        if (mediaInfo == null) {
            return;
        }
        final String title = mediaInfo.titleDescProperty().getValue();
        mediaView.snapshot(new Callback<SnapshotResult, Void>() {

            @Override
            public Void call(SnapshotResult result) {
                Image image = result.getImage();
                if (image == null) {
                    return null;
                }
                if (!saveSnapshotService.isRunning()) {
                    snapshotImageInfoProperty.setValue(new ImageInfo(image, title));
                    saveSnapshotService.restart();
                }
                return null;
            }
        }, null, null);
    }

    //----------------------------------------------------------------------------------
    // load list, save image services
    //-------------------------

    private final Service<?> loadDirectoryService;

    private final Service<?> loadFilesService;

    private final ObjectProperty<List<? extends File>> filesProperty = new SimpleObjectProperty<>();

    private final Service<?> loadListFileService;

    private final Service<?> loadUrlService;

    private final Service<?> loadUrlAutoStartHeadService;

    private final Service<?> loadUrlAutoStartTailService;

    private final Service<?> saveTagImageService;

    private final ObjectProperty<ImageInfo> snapshotImageInfoProperty = new SimpleObjectProperty<>();

    private final Service<?> saveSnapshotService;

    private final Service<?> stageCloseService;

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
                    public void callback(File dir, List<? extends MediaInfo> infos) {
                        if (infos == null) {
                            log("Load Directory Failure : " + dir.getAbsolutePath());
                            return;
                        }
                        log("Load Directory : " + dir.getAbsolutePath());
                        updateItems(dir.toString(), infos);
                        loadMedia();
                    }
                });
        loadFilesService = new LoadFilesService(filesProperty, metaChangeListener,
                new LoadFilesService.ICallback() {

                    @Override
                    public void callback(List<? extends File> files, List<? extends MediaInfo> infos) {
                        if (infos == null) {
                            log("Load Files Failure : " + Arrays.toString(files.toArray()));
                            return;
                        }
                        log("Load Files : " + Arrays.toString(files.toArray()));
                        appendItems(infos);
                        loadMedia();
                    }
                });
        loadListFileService = new LoadListFileService(stageProperty, metaChangeListener,
                settings.loadDirection, new LoadListFileService.ICallback() {

                    @Override
                    public void callback(File file, List<? extends MediaInfo> infos) {
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
                settings.loadDirection, new LoadUrlService.ICallback() {

                    @Override
                    public void callback(String text, String url, List<? extends MediaInfo> infos) {
                        if (infos == null) {
                            log("Load URL Failure : " + url);
                            return;
                        }
                        log("Load URL : " + url);
                        updateItems(text, infos);
                        loadMedia();
                    }
                });
        loadUrlAutoStartHeadService = new LoadUrlService(pathTextProperty, metaChangeListener,
                settings.loadDirection, new LoadUrlService.ICallback() {

                    @Override
                    public void callback(String text, String url, List<? extends MediaInfo> infos) {
                        if (infos == null) {
                            log("Load URL Failure : " + url);
                            return;
                        }
                        log("Load URL : " + url);
                        updateItems(text, infos);
                        loadMedia();
                        controlPlayerLater(Control.NEXT, null, true, settings.skipOnError.get(),
                                true);
                    }
                });
        loadUrlAutoStartTailService = new LoadUrlService(pathTextProperty, metaChangeListener,
                settings.loadDirection, new LoadUrlService.ICallback() {

                    @Override
                    public void callback(String text, String url, List<? extends MediaInfo> infos) {
                        if (infos == null) {
                            log("Load URL Failure : " + url);
                            return;
                        }
                        log("Load URL : " + url);
                        updateItems(text, infos);
                        loadMedia();
                        controlPlayerLater(Control.BACK, null, true, settings.skipOnError.get(),
                                true);
                    }
                });
        ObservableValue<ImageInfo> tagImageInfoProperty = new SimpleObjectProperty<ImageInfo>() {

            @Override
            public ImageInfo getValue() {
                MediaInfo info = currentInfoProperty.getValue();
                if (info == null) {
                    return null;
                }
                return new ImageInfo(info.imageProperty().getValue(), info.titleDescProperty()
                        .getValue());
            }
        };
        saveTagImageService = new SaveImageService(tagImageInfoProperty, stageProperty,
                new SaveImageService.ICallback() {

                    @Override
                    public void callback(File in, File out, ImageInfo info) {
                        if (out == null) {
                            log("Save Image Failure : " + in.getAbsolutePath());
                            return;
                        }
                        log("Save Image : " + out.getAbsolutePath());
                    }
                });
        saveSnapshotService = new SaveImageService(snapshotImageInfoProperty, stageProperty,
                new SaveImageService.ICallback() {

                    @Override
                    public void callback(File in, File out, ImageInfo info) {
                        if (out == null) {
                            log("Save Snapshot Failure : " + in.getAbsolutePath());
                            return;
                        }
                        log("Save Snapshot : " + out.getAbsolutePath());
                    }
                });
        stageCloseService = new StageCloseService(stageProperty);
    }

    private boolean loadRunning() {
        return loadDirectoryService.isRunning() || loadListFileService.isRunning()
                || loadUrlService.isRunning() || loadUrlAutoStartHeadService.isRunning()
                || loadUrlAutoStartTailService.isRunning();
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
                    logStage.toggleAutoFront();
                }

                @Override
                public void controlTab() {
                    SingleSelectionModel<Tab> selection = videoTab.getTabPane().getSelectionModel();
                    if (selection.isEmpty()) {
                        return;
                    }
                    if (videoTab.isSelected()) {
                        selection.select(infoTab);
                    } else {
                        selection.select(videoTab);
                    }
                }

                @Override
                public void controlTabSide() {
                    settings.toggleVislbleTabSide();
                }

                @Override
                public void controlVideoInfoSide() {
                    settings.toggleVisibleVideoInfoSide();
                }

                @Override
                public void controlVideoInfoWidth(double width) {
                    settings.setVideoInfoWidth(width);
                }

                @Override
                public void controlVideoInfoWidthPlus(double width) {
                    settings.setVideoInfoWidthPlus(width);
                }

                @Override
                public void controlVideoInfoWidthMinus(double width) {
                    controlVideoInfoWidthPlus(Double.isNaN(width) ? width : width * -1);
                }

                @Override
                public void controlVideoInfoHeight(double height) {
                    settings.setVideoInfoHeight(height);
                }

                @Override
                public void controlVideoInfoHeightPlus(double height) {
                    settings.setVideoInfoHeightPlus(height);
                }

                @Override
                public void controlVideoInfoHeightMinus(double height) {
                    controlVideoInfoHeightPlus(Double.isNaN(height) ? height : height * -1);
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
                public void controlRepeat(NextMode next) {
                    if (next == null) {
                        settings.toggleNextMode();
                    } else {
                        settings.nextMode.setValue(next);
                    }
                }

                @Override
                public void controlDirection(OtherDirection direction) {
                    if (direction == null) {
                        settings.toggleOtherDirection();
                    } else {
                        settings.otherDirection.setValue(direction);
                    }
                }

                @Override
                public void controlHead() {
                    PlayList.this.controlHead();
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
                    Slider controlTime = bufferSlider.getSlider();
                    double value = seconds * 1000;
                    controlTime.setValue(Math.max(controlTime.getMin(),
                            Math.min(value, controlTime.getMax() - 1)));
                }

                @Override
                public void controlTimePlus(long seconds) {
                    Slider controlTime = bufferSlider.getSlider();
                    double value = controlTime.getValue() + (seconds * 1000);
                    controlTime.setValue(Math.max(controlTime.getMin(),
                            Math.min(value, controlTime.getMax() - 1)));
                }

                @Override
                public void controlTimeMinus(long seconds) {
                    controlTimePlus(seconds * -1);
                }

                @Override
                public void controlTimeTail(long seconds) {
                    Slider controlTime = bufferSlider.getSlider();
                    double value = controlTime.getMax() - (seconds * 1000);
                    controlTime.setValue(Math.max(controlTime.getMin(),
                            Math.min(value, controlTime.getMax() - 1)));
                }

                @Override
                public void controlMute() {
                    settings.toggleMute();
                }

                @Override
                public void controlVolume(int volume) {
                    double value = volume / 100d;
                    settings.volume.set(Math.max(controlVolume.getMin(),
                            Math.min(value, controlVolume.getMax())));
                }

                @Override
                public void controlVolumePlus(int volume) {
                    double value = settings.volume.get() + (volume / 100d);
                    settings.volume.set(Math.max(controlVolume.getMin(),
                            Math.min(value, controlVolume.getMax())));
                }

                @Override
                public void controlVolumeMinus(int volume) {
                    controlVolumePlus(volume * -1);
                }

                @Override
                public void controlVolumeTail(int volume) {
                    double value = controlVolume.getMax() - (volume / 100d);
                    settings.volume.set(Math.max(controlVolume.getMin(),
                            Math.min(value + 0.001, controlVolume.getMax())));
                }

                @Override
                public void controlJump(int value) {
                    List<MediaInfo> infos = getItemsSnapshot();
                    int size = infos.size();
                    if (size == 0) {
                        return;
                    }
                    int idx = Math.min(Math.max(value, 1), size) - 1;
                    controlPlayerForceWithSkip(Control.SPECIFY, infos.get(idx));
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
                    controlPlayerForceWithSkip(Control.SPECIFY, infos.get(idx));
                }

                @Override
                public void controlJumpMinus(int value) {
                    controlJumpPlus(value * -1);
                }

                @Override
                public void controlRemove(int value) {
                    List<MediaInfo> infos = getItemsSnapshot();
                    int size = infos.size();
                    if (size == 0) {
                        return;
                    }
                    int idx = Math.min(Math.max(value, 1), size) - 1;
                    PlayList.this.controlRemove(infos.get(idx));
                }

                @Override
                public void controlRemovePlus(int value) {
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
                    PlayList.this.controlRemove(infos.get(idx));
                }

                @Override
                public void controlRemoveMinus(int value) {
                    controlRemovePlus(value * -1);
                }

                @Override
                public void controlSaveFile() {
                    // TODO
                    // save list file
                    // no implement now
                }

                @Override
                public void controlSaveImage() {
                    PlayList.this.controlSaveImage();
                }

                @Override
                public void controlSaveSnapshot() {
                    PlayList.this.controlSaveSnapshot();
                }

                @Override
                public void controlHideHeader() {
                    PlayList.this.controlHideHeader();
                }

                @Override
                public void controlHideFooter() {
                    PlayList.this.controlHideFooter();
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

                @Override
                public void controlOther(String[] args) {
                    String arg0 = (0 < args.length) ? args[0] : null;
                    String arg1 = (1 < args.length) ? args[1] : null;
                    if (arg0 != null) {
                        switch (arg0) {
                            case "pn":
                                movePageIfEnable(1, false);
                                break;
                            case "pb":
                                movePageIfEnable(-1, false);
                                break;
                            case "reset":
                                settings.reset();
                                break;
                            case "video":
                                settings.setVideoMode(videoTab, true);
                                break;
                            case "movie":
                                settings.setVideoMode(videoTab, false);
                                break;
                            case "audio":
                                settings.setAudioMode(infoTab);
                                break;
                            case "ncm":
                                UrlDispathcer.setNiconicoMail(arg1);
                                log("Set Niconico mail : " + arg1);
                                break;
                            case "ncp":
                                UrlDispathcer.setNiconicoPass(arg1);
                                log("Set Niconico pass : ***** ");
                                break;
                        }
                    }
                }
            });

    //----------------------------------------------------------------------------------
    // ORETOKU funny functions.
    //-------------------------

    private boolean movePageIfEnable(int inc, boolean autoStart) {
        String text = pathText.getText().trim();
        String url = LoadUrlService.getQueryPageUrl(text, inc);
        if ((url != null) && !url.isEmpty() && !text.equals(url)) {
            if (!loadRunning()) {
                pathText.setText(url);
                if (autoStart) {
                    if (0 <= inc) {
                        loadUrlAutoStartHeadService.restart();
                    } else {
                        loadUrlAutoStartTailService.restart();
                    }
                } else {
                    loadUrlService.restart();
                }
            }
            return true;
        }
        return false;
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
        if (logging) {
            logStage.appendLog(String.format("%1$tH:%1$tM:%1$tS:%1$tL - %2$s%n", new Date(), str));
        }
    }
}
