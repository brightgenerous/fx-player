package com.brightgenerous.fxplayer.application.playlist;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.SceneBuilder;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxBuilder;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;

class LogStage extends Stage {

    private final CheckBox frontCheck;

    private final TextArea logText;

    public LogStage(String title, ObservableList<Image> icons, EventHandler<KeyEvent> keyHandler) {

        logText = TextAreaBuilder.create().wrapText(true).editable(false).focusTraversable(false)
                .opacity(0.9).build();

        frontCheck = CheckBoxBuilder.create().text("_Auto Front").mnemonicParsing(true).build();

        Label label = LabelBuilder
                .create()
                .text("brigen fx-player 「Narudake Player」, Copyright(c) 2013 BrightGenerous, All Rights Reserved.")
                .build();
        VBox.setVgrow(logText, Priority.ALWAYS);

        final Parent parent = VBoxBuilder.create().children(logText, frontCheck, label).spacing(5)
                .padding(new Insets(5)).style("-fx-background-color:rgb(224, 224, 224);").build();

        {
            logText.setOnKeyTyped(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    Event.fireEvent(parent, event);
                }
            });
            if (keyHandler != null) {
                parent.addEventHandler(KeyEvent.KEY_TYPED, keyHandler);
            }
        }

        StageBuilder.create().width(580).height(360).icons(icons).title(title)
                .scene(SceneBuilder.create().root(parent).build()).applyTo(this);

        initModality(Modality.NONE);
    }

    public void appendLog(String str) {
        if (frontCheck.isSelected()) {
            toFront();
        }
        if (300000 < logText.getLength()) {
            logText.deleteText(0, 100000);
        }
        logText.appendText(str);
    }
}
