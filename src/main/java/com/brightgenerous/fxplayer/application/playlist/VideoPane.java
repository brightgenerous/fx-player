package com.brightgenerous.fxplayer.application.playlist;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaView;

public class VideoPane extends Pane {

    public static enum InfoSide {
        LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM;
    }

    {
        getStyleClass().add("video-pane");
    }

    private TableView<?> infoList;

    private final BooleanProperty visibleInfoList = new SimpleBooleanProperty();
    {
        visibleInfoList.addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                    Boolean newValue) {
                if (infoList == null) {
                    return;
                }
                List<Node> children = getChildren();
                if (newValue.booleanValue()) {
                    if (!children.contains(infoList)) {
                        children.add(infoList);
                    }
                } else {
                    if (children.contains(infoList)) {
                        children.remove(infoList);
                    }
                }
                requestLayout();
            }
        });
    }

    private final ObjectProperty<InfoSide> infoSideProperty = new SimpleObjectProperty<>();
    {
        infoSideProperty.addListener(new ChangeListener<InfoSide>() {

            @Override
            public void changed(ObservableValue<? extends InfoSide> observable, InfoSide oldValue,
                    InfoSide newValue) {
                requestLayout();
            }
        });
    }

    @Override
    protected void layoutChildren() {

        Node video = getMediaView(true);
        Node list = getVisibleInfoList() ? getInfoList(true) : null;

        if ((video == null) && (list == null)) {
            // video is null and list is null
            return;
        }

        double rightInset = snapSpace(getInsets().getRight());
        double leftInset = snapSpace(getInsets().getLeft());
        double topInset = snapSpace(getInsets().getTop());
        double bottomInset = snapSpace(getInsets().getBottom());

        double actualWidth = getWidth() - leftInset - rightInset;
        double actualHeight = getHeight() - topInset - bottomInset;

        if (video == null) {
            // video is null and list is not null
            layoutInArea(list, leftInset, topInset, actualWidth, actualHeight, 0, HPos.CENTER,
                    VPos.CENTER);
            return;
        }

        double videoWidth = 0;
        double videoHeight = 0;
        {
            Bounds bounds = video.getBoundsInParent();
            if (bounds == null) {
                bounds = video.getBoundsInLocal();
            }
            if (bounds == null) {
                videoWidth = actualWidth;
                videoHeight = actualHeight;
            } else {
                videoWidth = bounds.getWidth();
                videoHeight = bounds.getHeight();
            }
        }

        if (list == null) {
            layoutInArea(video, leftInset + ((actualWidth - videoWidth) / 2), topInset
                    + ((actualHeight - videoHeight) / 2), videoWidth, videoHeight, 0, HPos.CENTER,
                    VPos.CENTER);
            return;
        }

        double listWidth = actualWidth;
        double listHeight = actualHeight;
        boolean horizon = true;
        {
            double widthSpace = actualWidth - videoWidth;
            double heightSpace = actualHeight - videoHeight;
            if (heightSpace <= widthSpace) {
                listWidth = widthSpace;
            } else {
                listHeight = heightSpace;
                horizon = false;
            }
        }

        InfoSide side = getInfoSide();
        if (side == null) {
            side = InfoSide.RIGHT_BOTTOM;
        }

        if (side == InfoSide.LEFT_TOP) {
            if (horizon) {
                layoutInArea(video, leftInset + listWidth, topInset, videoWidth, videoHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            } else {
                layoutInArea(video, leftInset, topInset + listHeight, videoWidth, videoHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            }

            layoutInArea(list, leftInset, topInset, listWidth, listHeight, 0, HPos.CENTER,
                    VPos.CENTER);
        } else if (side == InfoSide.RIGHT_BOTTOM) {
            layoutInArea(video, leftInset, topInset, videoWidth, videoHeight, 0, HPos.CENTER,
                    VPos.CENTER);

            if (horizon) {
                layoutInArea(list, leftInset + videoWidth, topInset, listWidth, listHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            } else {
                layoutInArea(list, leftInset, topInset + videoHeight, listWidth, listHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            }
        } else if (side == InfoSide.LEFT_BOTTOM) {
            if (horizon) {
                layoutInArea(video, leftInset + listWidth, topInset, videoWidth, videoHeight, 0,
                        HPos.CENTER, VPos.CENTER);
                layoutInArea(list, leftInset, topInset, listWidth, listHeight, 0, HPos.CENTER,
                        VPos.CENTER);
            } else {
                layoutInArea(video, leftInset, topInset, videoWidth, videoHeight, 0, HPos.CENTER,
                        VPos.CENTER);
                layoutInArea(list, leftInset, topInset + videoHeight, listWidth, listHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            }
        } else if (side == InfoSide.RIGHT_TOP) {
            if (horizon) {
                layoutInArea(video, leftInset, topInset, videoWidth, videoHeight, 0, HPos.CENTER,
                        VPos.CENTER);
                layoutInArea(list, leftInset + videoWidth, topInset, listWidth, listHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            } else {
                layoutInArea(video, leftInset, topInset + listHeight, videoWidth, videoHeight, 0,
                        HPos.CENTER, VPos.CENTER);
                layoutInArea(list, leftInset, topInset, listWidth, listHeight, 0, HPos.CENTER,
                        VPos.CENTER);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public MediaView getMediaView() {
        return getMediaView(false);
    }

    private MediaView getMediaView(boolean managed) {
        MediaView ret = null;
        List<Node> children = managed ? getManagedChildren() : getChildren();
        for (Node node : children) {
            if (node instanceof MediaView) {
                ret = (MediaView) node;
            }
        }
        return ret;
    }

    public void setMediaView(MediaView view) {
        List<MediaView> dels = new ArrayList<>();
        List<Node> children = getChildren();
        for (Node node : children) {
            if (node instanceof MediaView) {
                dels.add((MediaView) node);
            }
        }
        if (!dels.isEmpty()) {
            children.removeAll(dels);
        }
        if (view != null) {
            children.add(0, view);
        }
    }

    public TableView<?> getInfoList() {
        return getInfoList(false);
    }

    private TableView<?> getInfoList(boolean managed) {
        TableView<?> ret = null;
        List<Node> children = managed ? getManagedChildren() : getChildren();
        for (Node node : children) {
            if (node instanceof TableView) {
                ret = (TableView<?>) node;
            }
        }
        return ret;
    }

    public void setInfoList(TableView<?> view) {
        List<TableView<?>> dels = new ArrayList<>();
        List<Node> children = getChildren();
        for (Node node : children) {
            if (node instanceof TableView) {
                dels.add((TableView<?>) node);
            }
        }
        if (!dels.isEmpty()) {
            children.removeAll(dels);
        }
        infoList = view;
        if ((view != null) && visibleInfoList.get()) {
            children.add(view);
        }
    }

    public BooleanProperty visibleInfoListProperty() {
        return visibleInfoList;
    }

    public boolean getVisibleInfoList() {
        return visibleInfoList.get();
    }

    public void setVisibleInfoList(boolean visible) {
        visibleInfoList.set(visible);
    }

    public ObjectProperty<InfoSide> infoSideProperty() {
        return infoSideProperty;
    }

    public InfoSide getInfoSide() {
        return infoSideProperty.getValue();
    }

    public void setInfoSide(InfoSide infoSide) {
        infoSideProperty.setValue(infoSide);
    }
}
