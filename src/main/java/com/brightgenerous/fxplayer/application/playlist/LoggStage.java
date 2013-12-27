package com.brightgenerous.fxplayer.application.playlist;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.WindowEvent;

import com.brightgenerous.fxplayer.application.FxUtils;
import com.brightgenerous.fxplayer.application.FxUtils.LoadData;

class LoggStage extends Stage {

    private final Logg controller;

    public LoggStage(final Stage owner, String title, ObservableList<Image> icons,
            EventHandler<KeyEvent> keyHandler) {

        LoadData<Logg> loadData = FxUtils.load(Logg.class, this);

        controller = loadData.getController();

        Parent root = loadData.getRoot();
        StageBuilder.create().icons(icons).title(title)
                .scene(SceneBuilder.create().root(root).build()).applyTo(this);
        if (keyHandler != null) {
            root.addEventHandler(KeyEvent.KEY_TYPED, keyHandler);
        }

        addEventHandler(WindowEvent.WINDOW_SHOWING, new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                if (Double.isNaN(getX()) || Double.isNaN(getY())) {
                    double sceneX = 0;
                    double sceneY = 0;
                    Scene scene = owner.getScene();
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
                    setX(owner.getX() + sceneX);
                    setY(owner.getY() + sceneY);
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
        controller.appendLog(str);
    }

    public boolean toggleAutoFront() {
        return controller.toggleAutoFront();
    }
}
