package com.brightgenerous.fxplayer.application.playlist;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import com.brightgenerous.fxplayer.application.FxUtils;
import com.brightgenerous.fxplayer.application.FxUtils.Inject;

public class Logg implements Initializable {

    @Inject
    private Stage stage;

    @FXML
    private Pane rootPane;

    @FXML
    private TextArea logText;

    @FXML
    private CheckBox autoFrontControl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (FxUtils.isESC(event)) {
                    if (stage.isShowing()) {
                        stage.close();
                        event.consume();
                    }
                }
            }
        });

        {
            logText.setOnKeyTyped(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    Event.fireEvent(rootPane, event);
                }
            });
        }
    }

    public void appendLog(String str) {
        if (autoFrontControl.isSelected()) {
            stage.toFront();
        }
        if (300000 < logText.getLength()) {
            logText.deleteText(0, 100000);
        }
        logText.appendText(str);
    }
}
