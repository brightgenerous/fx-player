package com.brightgenerous.fxplayer.application;

import java.util.Locale;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;

import com.brightgenerous.fxplayer.application.playlist.PlayList;

public class FxPlayerApplication extends Application {

    public static void start(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        String title = ResourceBundle.getBundle(getClass().getName(), Locale.getDefault())
                .getString("title");
        StageBuilder.create().title(title).applyTo(stage);
        Utils.move(stage, PlayList.class);
        stage.show();
    }
}
