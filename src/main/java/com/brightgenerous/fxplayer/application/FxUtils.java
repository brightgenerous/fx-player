package com.brightgenerous.fxplayer.application;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.PaneBuilder;
import javafx.stage.Stage;
import javafx.util.Callback;

public class FxUtils {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Inject {
    }

    public static class LoadData<T> {

        private final T controller;

        private final Parent root;

        LoadData(T controller, Parent root) {
            this.controller = controller;
            this.root = root;
        }

        public T getController() {
            return controller;
        }

        public Parent getRoot() {
            return root;
        }
    }

    private FxUtils() {
    }

    public static void scene(Stage stage, Class<?> clazz) {
        LoadData<?> loadData = load(clazz, stage);
        Parent root = loadData.getRoot();
        Scene scene = SceneBuilder.create().root(root).build();
        scene.getStylesheets().addAll(root.getStylesheets());
        stage.setScene(scene);
    }

    public static <T> LoadData<T> load(Class<? extends T> clazz, Stage stage) {
        FXMLLoader loader = new FXMLLoader();
        {
            ResourceBundle bundle = null;
            try {
                bundle = ResourceBundle.getBundle(clazz.getName(), Locale.getDefault());
            } catch (MissingResourceException e) {
            }
            if (bundle != null) {
                loader.setResources(bundle);
            }
            URL fxml = clazz.getResource(clazz.getSimpleName() + ".fxml");
            if (fxml != null) {
                loader.setLocation(fxml);
            }
        }

        T controller = null;
        {
            final Callback<Class<?>, Object> factory = loader.getControllerFactory();
            if (factory != null) {
                controller = (T) factory.call(clazz);
            }
            if (controller == null) {
                try {
                    controller = clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (controller != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getAnnotation(Inject.class) == null) {
                        continue;
                    }
                    if (field.getType().isAssignableFrom(Stage.class)) {
                        try {
                            if (!Modifier.isPublic(field.getModifiers()) && !field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            field.set(controller, stage);
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            if (controller != null) {
                loader.setController(controller);
            }
        }

        Parent root;
        {
            Object obj;
            try {
                obj = loader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (obj instanceof Parent) {
                root = (Parent) obj;
            } else if (obj instanceof Node) {
                root = PaneBuilder.create().children((Node) obj).build();
            } else {
                throw new RuntimeException("loaded instance is unknown. "
                        + obj.getClass().getName());
            }
        }

        return new LoadData<>(controller, root);
    }

    public static boolean isESC(KeyEvent event) {
        boolean esc = event.getCode() == KeyCode.ESCAPE;
        if (!esc) {
            String str = event.getCharacter();
            if (!str.isEmpty()) {
                esc = str.charAt(0) == 27;
            }
        }
        return esc;
    }
}
