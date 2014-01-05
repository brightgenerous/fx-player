package com.brightgenerous.fxplayer.application.playlist;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import com.brightgenerous.fxplayer.application.FxUtils;

public class LogStage extends Stage implements Initializable {

    private final ObservableValue<? extends Stage> owner;

    private final EventHandler<? super KeyEvent> keyHandler;

    @FXML
    private Pane rootPane;

    @FXML
    private TextArea logText;

    @FXML
    private CheckBox autoFrontControl;

    public LogStage(ObservableValue<? extends Stage> owner,
            EventHandler<? super KeyEvent> keyHandler) {
        this.owner = owner;
        this.keyHandler = keyHandler;

        FXMLLoader loader = new FXMLLoader();
        {
            Class<?> clazz = getClass();
            ResourceBundle bundle = null;
            try {
                bundle = ResourceBundle.getBundle(clazz.getName(), Locale.getDefault());
            } catch (MissingResourceException e) {
            }
            if (bundle != null) {
                loader.setResources(bundle);
            }
            URL fxml = clazz.getResource(clazz.getSimpleName() + ".fxml");
            if (fxml == null) {
                fxml = clazz.getResource(LogStage.class.getSimpleName() + ".fxml");
            }
            if (fxml != null) {
                loader.setLocation(fxml);
            }
        }
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (FxUtils.isESC(event)) {
                    if (isShowing()) {
                        close();
                        event.consume();
                    }
                }
            }
        });

        {
            logText.setOnKeyTyped(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    Event.fireEvent(rootPane, event);
                }
            });
        }

        StageBuilder.create().icons(owner.getValue().getIcons())
                .title(owner.getValue().getTitle().concat(resources.getString("title.suffix")))
                .scene(SceneBuilder.create().root(rootPane).build()).applyTo(this);
        if (keyHandler != null) {
            rootPane.addEventHandler(KeyEvent.KEY_TYPED, keyHandler);
        }

        addEventHandler(WindowEvent.WINDOW_SHOWING, new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                if (Double.isNaN(getX()) || Double.isNaN(getY())) {
                    double sceneX = 0;
                    double sceneY = 0;
                    Window window = owner.getValue();
                    Scene scene = window.getScene();
                    if (scene != null) {
                        sceneX = scene.getX();
                        sceneY = scene.getY();
                    }
                    if (Double.isNaN(sceneX)) {
                        sceneX = 0;
                    }
                    if (Double.isNaN(sceneY)) {
                        sceneY = 0;
                    }
                    setX(window.getX() + sceneX);
                    setY(window.getY() + sceneY);
                }
            }
        });
        addEventHandler(WindowEvent.WINDOW_HIDDEN, new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                setX(getX());
                setY(getY());
            }
        });

        initModality(Modality.NONE);
    }

    public void appendLog(String str) {
        if (autoFrontControl.isSelected()) {
            toFront();
        }
        if (300000 < logText.getLength()) {
            logText.deleteText(0, 100000);
        }
        logText.appendText(str);
    }

    public boolean toggleAutoFront() {
        boolean ret = autoFrontControl.isSelected();
        autoFrontControl.setSelected(!ret);
        return ret;
    }
}
