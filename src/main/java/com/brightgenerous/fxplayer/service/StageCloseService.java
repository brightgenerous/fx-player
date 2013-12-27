package com.brightgenerous.fxplayer.service;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import com.brightgenerous.fxplayer.application.FxUtils;
import com.brightgenerous.fxplayer.application.FxUtils.LoadData;

public class StageCloseService extends Service<Void> {

    private final ObjectProperty<Stage> owner;

    private Stage dialog;

    private final Runnable runnable = new Runnable() {

        @Override
        public void run() {
            if (dialog == null) {
                final Stage stage = StageBuilder.create().style(StageStyle.UTILITY)
                        .resizable(false).build();
                stage.initOwner(owner.getValue());
                stage.initModality(Modality.APPLICATION_MODAL);

                LoadData<StageClose> loadData = FxUtils.load(StageClose.class, stage);
                stage.setScene(SceneBuilder.create().root(loadData.getRoot()).build());

                loadData.getController().addOkEventHandler(ActionEvent.ACTION,
                        new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                owner.getValue().close();
                                dialog.close();
                                Platform.exit();
                            }
                        });

                stage.addEventHandler(WindowEvent.WINDOW_SHOWING, new EventHandler<WindowEvent>() {

                    @Override
                    public void handle(WindowEvent event) {
                        double sceneX = 0;
                        double sceneY = 0;
                        Scene scene = owner.getValue().getScene();
                        if (scene != null) {
                            sceneX = scene.getX();
                            sceneY = scene.getY();
                            if (Double.isNaN(sceneX)) {
                                sceneX = 0;
                            }
                            if (Double.isNaN(sceneY)) {
                                sceneY = 0;
                            }
                        }
                        stage.setX(owner.getValue().getX() + sceneX);
                        stage.setY(owner.getValue().getY() + sceneY);
                    }
                });

                dialog = stage;
            }

            dialog.showAndWait();
        }
    };

    public StageCloseService(ObjectProperty<Stage> stage) {
        owner = stage;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                if (isCancelled()) {
                    return null;
                }
                if (Platform.isFxApplicationThread()) {
                    runnable.run();
                } else {
                    Platform.runLater(runnable);
                }
                return null;
            }
        };
    }
}
