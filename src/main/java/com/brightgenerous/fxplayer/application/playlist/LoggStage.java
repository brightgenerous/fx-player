package com.brightgenerous.fxplayer.application.playlist;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.SceneBuilder;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;

import com.brightgenerous.fxplayer.application.FxUtils;
import com.brightgenerous.fxplayer.application.FxUtils.LoadData;

class LoggStage extends Stage {

    private final Logg controller;

    public LoggStage(String title, ObservableList<Image> icons, EventHandler<KeyEvent> keyHandler) {
        LoadData<Logg> loadData = FxUtils.load(Logg.class, this);

        controller = loadData.getController();

        Parent root = loadData.getRoot();
        StageBuilder.create().icons(icons).title(title)
                .scene(SceneBuilder.create().root(root).build()).applyTo(this);
        if (keyHandler != null) {
            root.addEventHandler(KeyEvent.KEY_TYPED, keyHandler);
        }

        initModality(Modality.NONE);
    }

    public void appendLog(String str) {
        controller.appendLog(str);
    }
}
