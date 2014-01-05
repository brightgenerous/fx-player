package com.brightgenerous.fxplayer.application.playlist;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

public class Spectrums extends HBox {

    @FXML
    private ProgressBar spectrumBar1;

    @FXML
    private ProgressBar spectrumBar2;

    @FXML
    private ProgressBar spectrumBar3;

    @FXML
    private ProgressBar spectrumBar4;

    public Spectrums() throws IOException {
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
                fxml = clazz.getResource(Spectrums.class.getSimpleName() + ".fxml");
            }
            if (fxml != null) {
                loader.setLocation(fxml);
            }
        }
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }

    public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes,
            float[] phases) {
        spectrumBar1.setProgress((magnitudes[0] + 60) / 60);
        spectrumBar2.setProgress((magnitudes[1] + 60) / 60);
        spectrumBar3.setProgress(phases[0] / Math.PI);
        spectrumBar4.setProgress(phases[1] / Math.PI);
    }
}
