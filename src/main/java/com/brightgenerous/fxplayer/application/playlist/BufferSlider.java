package com.brightgenerous.fxplayer.application.playlist;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;

public class BufferSlider extends StackPane implements Initializable {

    @FXML
    private Slider slider;

    @FXML
    private ProgressBar currentProgress;

    @FXML
    private ProgressBar bufferProgress;

    private final DoubleProperty buffer = new SimpleDoubleProperty();

    public BufferSlider() throws IOException {
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
                fxml = clazz.getResource(BufferSlider.class.getSimpleName() + ".fxml");
            }
            if (fxml != null) {
                loader.setLocation(fxml);
            }
        }
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentProgress.prefWidthProperty().bind(slider.prefWidthProperty());
        currentProgress.progressProperty()
                .bind(slider.valueProperty().divide(slider.maxProperty()));
        bufferProgress.prefWidthProperty().bind(slider.prefWidthProperty());
        bufferProgress.progressProperty().bind(buffer.divide(slider.maxProperty()));
    }

    public Slider getSlider() {
        return slider;
    }

    public void setBuffer(double value) {
        buffer.set(value);
    }
}
