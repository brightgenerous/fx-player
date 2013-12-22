package com.brightgenerous.fxplayer.application.playlist;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.SceneBuilder;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;

class LogStage extends Stage {

    private final CheckBox frontCheck;

    private final TextArea logText;

    public LogStage(String title, ObservableList<Image> icons) {

        logText = TextAreaBuilder.create().wrapText(true).editable(false).opacity(0.9).build();

        frontCheck = new CheckBox("auto front");

        Label label = LabelBuilder
                .create()
                .text("brigen fx-player 「Narudake Player」, Copyright(c) 2013 BrightGenerous, All Rights Reserved.")
                .build();
        VBox.setVgrow(logText, Priority.ALWAYS);

        Parent parent = VBoxBuilder.create().children(logText, frontCheck, label).spacing(5)
                .padding(new Insets(5)).style("-fx-background-color:rgb(224, 224, 224);").build();

        StageBuilder.create().width(580).height(360).icons(icons).title(title)
                .scene(SceneBuilder.create().root(parent).build()).applyTo(this);

        initModality(Modality.NONE);
    }

    public void appendLog(String str) {
        if (frontCheck.isSelected()) {
            toFront();
        }
        logText.appendText(str);
    }
}
