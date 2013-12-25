package com.brightgenerous.fxplayer.service;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import com.brightgenerous.fxplayer.application.FxUtils;
import com.brightgenerous.fxplayer.application.FxUtils.Inject;

public class StageClose implements Initializable {

    @Inject
    private Stage stage;

    @Inject
    private ResourceBundle bundle;

    @FXML
    private Pane rootPane;

    @FXML
    private Button controlOk;

    @FXML
    private Button controlCancel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        stage.setTitle(bundle.getString("stage.title"));

        controlOk.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                stage.hide();
            }
        });

        rootPane.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (FxUtils.isESC(event)) {
                    stage.hide();
                }
            }
        });

        stage.addEventHandler(WindowEvent.WINDOW_SHOWING, new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                controlCancel.requestFocus();
            }
        });
    }

    @FXML
    protected void controlCancel() {
        stage.hide();
    }

    public <T extends Event> void addOkEventHandler(EventType<T> type,
            EventHandler<? super T> handler) {
        controlOk.addEventHandler(type, handler);
    }
}
