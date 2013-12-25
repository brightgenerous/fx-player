package com.brightgenerous.fxplayer.application;

import java.util.Locale;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.WindowEvent;

import com.brightgenerous.fxplayer.application.playlist.PlayList;

public class FxPlayerApplication extends Application {

    public static void start(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        String title = ResourceBundle.getBundle(getClass().getName(), Locale.getDefault())
                .getString("title");
        String version = ResourceBundle.getBundle(getClass().getName(), Locale.getDefault())
                .getString("version");
        if ((version != null) && !version.isEmpty()) {
            title = title + " - " + version;
        }
        StageBuilder.create().title(title)
                .icons(new Image(getClass().getResourceAsStream("icon.png"))).applyTo(stage);

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
                System.exit(0);
            }
        });

        FxUtils.scene(stage, PlayList.class);
        stage.show();
    }
}
