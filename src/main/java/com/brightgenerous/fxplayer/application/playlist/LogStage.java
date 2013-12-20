package com.brightgenerous.fxplayer.application.playlist;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
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

public class LogStage extends Stage {

    private TextArea logText;

    public LogStage(String title, ObservableList<Image> icons) {
        Label label = LabelBuilder
                .create()
                .prefWidth(9999)
                .alignment(Pos.CENTER_RIGHT)
                .text("brigen fx-player 「Narudake Player」, Copyright(c) 2013 BrightGenerous, All Rights Reserved.")
                .build();
        logText = TextAreaBuilder.create().wrapText(true).editable(false).build();
        VBox.setVgrow(logText, Priority.ALWAYS);
        Parent parent = VBoxBuilder.create().children(logText, label).build();
        Scene scene = SceneBuilder.create().root(parent).build();
        StageBuilder.create().width(640).height(360).scene(scene).icons(icons).title(title)
                .applyTo(this);

        initModality(Modality.NONE);
    }

    public void appendLog(String str) {
        logText.appendText(str);
    }
}
