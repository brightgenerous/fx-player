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
import javafx.scene.SceneBuilder;
import javafx.scene.layout.PaneBuilder;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Utils {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Inject {
    }

    private Utils() {
    }

    public static void move(final Stage owner, Class<?> clazz) {
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
        {
            final Callback<Class<?>, Object> deleg = loader.getControllerFactory();
            loader.setControllerFactory(new Callback<Class<?>, Object>() {

                @Override
                public Object call(Class<?> clazz) {
                    Object ret = null;
                    if (deleg != null) {
                        ret = deleg.call(clazz);
                    }
                    if (ret == null) {
                        try {
                            ret = clazz.newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (ret != null) {
                        for (Field field : clazz.getDeclaredFields()) {
                            if (field.getAnnotation(Inject.class) == null) {
                                continue;
                            }
                            if (field.getType().isAssignableFrom(Stage.class)) {
                                try {
                                    if (!Modifier.isPublic(field.getModifiers())
                                            && !field.isAccessible()) {
                                        field.setAccessible(true);
                                    }
                                    field.set(ret, owner);
                                } catch (IllegalArgumentException | IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (field.getType().isAssignableFrom(ResourceBundle.class)) {
                                ResourceBundle bundle = ResourceBundle.getBundle(clazz.getName(),
                                        Locale.getDefault());
                                if (bundle != null) {
                                    try {
                                        if (!Modifier.isPublic(field.getModifiers())
                                                && !field.isAccessible()) {
                                            field.setAccessible(true);
                                        }
                                        field.set(ret, bundle);
                                    } catch (IllegalArgumentException | IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                    }
                    return ret;
                }
            });
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

        owner.setScene(SceneBuilder.create().root(root).build());
    }
}
