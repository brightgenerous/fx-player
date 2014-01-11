package com.brightgenerous.fxplayer.application.playlist;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
        LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM, OVERLAY;
    }

    {
        getStyleClass().add("video-pane");
    }

    private final BooleanProperty visibleInfoList = new SimpleBooleanProperty(this,
            "visibleInfoList");

    private final ObjectProperty<InfoSide> infoSide = new SimpleObjectProperty<>(this, "infoSide");

    private final DoubleProperty videoInfoMaxWidth = new SimpleDoubleProperty(this,
            "videoInfoMaxWidth");

    private final DoubleProperty videoInfoMaxHeight = new SimpleDoubleProperty(this,
            "videoInfoMaxHeight");

    {
        visibleInfoList.addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                    Boolean newValue) {
                requestLayout();
            }
        });

        infoSide.addListener(new ChangeListener<InfoSide>() {

            @Override
            public void changed(ObservableValue<? extends InfoSide> observable, InfoSide oldValue,
                    InfoSide newValue) {
                requestLayout();
            }
        });

        videoInfoMaxWidth.addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                    Number newValue) {
                requestLayout();
            }
        });

        videoInfoMaxHeight.addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                    Number newValue) {
                requestLayout();
            }
        });
    }

    public VideoPane() {
        setMediaView(null);
    }

    @Override
    protected void layoutChildren() {

        Node video = getMediaView(true);
        Node list = getInfoList(true);

        if ((video == null) && (list == null)) {
            // video is null and list is null
            return;
        }

        double leftInset = snapSpace(getInsets().getLeft());
        double topInset = snapSpace(getInsets().getTop());

        double width = getWidth();
        double actualWidth = width - leftInset - snapSpace(getInsets().getRight());
        double actualHeight = getHeight() - topInset - snapSpace(getInsets().getBottom());

        if ((list != null) && !getVisibleInfoList()) {
            // list is invisible
            layoutInArea(list, width, topInset, 0, 0, 0, HPos.LEFT, VPos.CENTER);
            list = null;
        }
        if (video == null) {
            if (list != null) {
                // video is null and list is not null
                layoutInArea(list, leftInset, topInset, actualWidth, actualHeight, 0, HPos.CENTER,
                        VPos.CENTER);
            }
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

        InfoSide side = getInfoSide();
        if (side == null) {
            side = InfoSide.OVERLAY;
        }

        if ((list == null) || (side == InfoSide.OVERLAY)) {
            layoutInArea(video, leftInset + ((actualWidth - videoWidth) / 2), topInset
                    + ((actualHeight - videoHeight) / 2), videoWidth, videoHeight, 0, HPos.CENTER,
                    VPos.CENTER);
            if (list != null) {
                layoutInArea(list, leftInset, topInset, actualWidth, actualHeight, 0, HPos.CENTER,
                        VPos.CENTER);
            }
            return;
        }

        double listWidth;
        double listHeight;
        boolean horizon;
        {
            double widthSpace = actualWidth - videoWidth;
            double heightSpace = actualHeight - videoHeight;
            double maxWidth = videoInfoMaxWidth.get();
            double maxHeight = videoInfoMaxHeight.get();
            boolean widthNan = Double.isNaN(maxWidth);
            boolean heightNan = Double.isNaN(maxHeight);
            if (!widthNan) {
                widthSpace = Math.min(widthSpace, maxWidth);
            }
            if (!heightNan) {
                heightSpace = Math.min(heightSpace, maxHeight);
            }
            if (widthNan == heightNan) {
                // (widthNan, heightNan) => (true, true) or (false, false)
                if ((heightSpace / actualHeight) <= (widthSpace / actualWidth)) {
                    listWidth = widthSpace;
                    listHeight = actualHeight;
                    horizon = true;
                } else {
                    listWidth = actualWidth;
                    listHeight = heightSpace;
                    horizon = false;
                }
            } else if (widthNan) {
                // (widthNan, heightNan) => (true, false)
                listWidth = actualWidth;
                listHeight = heightSpace;
                horizon = false;
            } else {
                // (widthNan, heightNan) => (false, true)
                listWidth = widthSpace;
                listHeight = actualHeight;
                horizon = true;
            }
        }

        double videoLeftInset;
        double videoTopInset;
        {
            if (horizon) {
                videoLeftInset = (actualWidth - listWidth - videoWidth) / 2;
                videoTopInset = (actualHeight - videoHeight) / 2;
            } else {
                videoLeftInset = (actualWidth - videoWidth) / 2;
                videoTopInset = (actualHeight - listHeight - videoHeight) / 2;
            }
        }

        double videoLeft;
        double videoTop;
        double listLeft;
        double listTop;

        if (horizon) {
            if ((side == InfoSide.LEFT_TOP) || (side == InfoSide.LEFT_BOTTOM)) {
                // video => right , info => left
                videoLeft = leftInset + listWidth + videoLeftInset;
                videoTop = topInset + videoTopInset;
                listLeft = leftInset;
                listTop = topInset;
            } else {
                // video => left , info => right
                videoLeft = leftInset + videoLeftInset;
                videoTop = topInset + videoTopInset;
                listLeft = leftInset + videoWidth + (videoLeftInset * 2);
                listTop = topInset;
            }
        } else {
            if ((side == InfoSide.LEFT_TOP) || (side == InfoSide.RIGHT_TOP)) {
                // video => bottom , info => top
                videoLeft = leftInset + videoLeftInset;
                videoTop = topInset + listHeight + videoTopInset;
                listLeft = leftInset;
                listTop = topInset;
            } else {
                // video => top , info => bottom
                videoLeft = leftInset + videoLeftInset;
                videoTop = topInset + videoTopInset;
                listLeft = leftInset;
                listTop = topInset + videoHeight + (videoTopInset * 2);
            }
        }

        layoutInArea(video, videoLeft, videoTop, videoWidth, videoHeight, 0, HPos.CENTER,
                VPos.CENTER);
        layoutInArea(list, listLeft, listTop, listWidth, listHeight, 0, HPos.CENTER, VPos.CENTER);
    }

    private final MediaView dummy = new MediaView();

    public MediaView getMediaView() {
        MediaView ret = getMediaView(false);
        if (ret == dummy) {
            ret = null;
        }
        return ret;
    }

    private MediaView getMediaView(boolean managed) {
        MediaView ret = null;
        List<Node> children = managed ? getManagedChildren() : getChildren();
        for (Node node : children) {
            if (node instanceof MediaView) {
                ret = (MediaView) node;
                break;
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
        if (view == null) {
            children.add(0, dummy);
        } else {
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
                break;
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
        if (view != null) {
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
        return infoSide;
    }

    public InfoSide getInfoSide() {
        return infoSide.getValue();
    }

    public void setInfoSide(InfoSide value) {
        infoSide.setValue(value);
    }

    public DoubleProperty videoInfoMaxWidthProperty() {
        return videoInfoMaxWidth;
    }

    public double getVideoInfoMaxWidth() {
        return videoInfoMaxWidth.get();
    }

    public void setVideoInfoMaxWidth(double width) {
        videoInfoMaxWidth.set(width);
    }

    public DoubleProperty videoInfoMaxHeightProperty() {
        return videoInfoMaxHeight;
    }

    public double getVideoInfoMaxHeight() {
        return videoInfoMaxHeight.get();
    }

    public void setVideoInfoMaxHeight(double height) {
        videoInfoMaxHeight.set(height);
    }
}
