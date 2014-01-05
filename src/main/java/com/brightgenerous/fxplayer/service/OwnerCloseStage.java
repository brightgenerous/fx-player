package com.brightgenerous.fxplayer.service;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import com.brightgenerous.fxplayer.application.FxUtils;

public class OwnerCloseStage extends Stage implements Initializable {

    private final ObservableValue<? extends Stage> owner;

    @FXML
    private Pane rootPane;

    @FXML
    private Button controlCancel;

    public OwnerCloseStage(final ObservableValue<? extends Stage> owner) {
        this.owner = owner;

        FXMLLoader loader = new FXMLLoader();
        {
            Class<?> clazz = getClass();
            ResourceBundle bundle = null;
            try {
                bundle = ResourceBundle.getBundle(clazz.getName(), Locale.getDefault());
            } catch (MissingResourceException e) {
            }
            if (bundle != null) {
                loader.setResources(bundle);
            }
            URL fxml = clazz.getResource(clazz.getSimpleName() + ".fxml");
            if (fxml == null) {
                fxml = clazz.getResource(OwnerCloseStage.class.getSimpleName() + ".fxml");
            }
            if (fxml != null) {
                loader.setLocation(fxml);
            }
        }
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        StageBuilder.create().style(StageStyle.UTILITY)
                .scene(SceneBuilder.create().root(rootPane).build())
                .title(resources.getString("stage.title")).resizable(false).applyTo(this);

        initOwner(owner.getValue());
        initModality(Modality.APPLICATION_MODAL);

        addEventHandler(WindowEvent.WINDOW_SHOWING, new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                double sceneX = 0;
                double sceneY = 0;
                Stage own = owner.getValue();
                Scene scene = own.getScene();
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
                setX(own.getX() + sceneX);
                setY(own.getY() + sceneY);
            }
        });

        rootPane.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (FxUtils.isESC(event)) {
                    hide();
                }
            }
        });

        addEventHandler(WindowEvent.WINDOW_SHOWING, new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                controlCancel.requestFocus();
            }
        });
    }

    @FXML
    protected void controlCancel() {
        hide();
    }

    @FXML
    protected void controlOk() {
        owner.getValue().close();
        close();
        Platform.exit();
    }
}
