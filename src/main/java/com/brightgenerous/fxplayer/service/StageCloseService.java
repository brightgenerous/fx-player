package com.brightgenerous.fxplayer.service;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class StageCloseService extends Service<Void> {

    private final ObservableValue<? extends Stage> owner;

    private Stage dialog;

    private final Runnable runnable = new Runnable() {

        @Override
        public void run() {
            if (dialog == null) {
                dialog = new CloseOwnerStage(owner);
            }
            dialog.showAndWait();
        }
    };

    public StageCloseService(ObservableValue<? extends Stage> stage) {
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
