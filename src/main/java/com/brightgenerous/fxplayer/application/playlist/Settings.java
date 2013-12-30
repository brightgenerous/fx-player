package com.brightgenerous.fxplayer.application.playlist;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.geometry.Side;

import com.brightgenerous.fxplayer.application.playlist.VideoPane.InfoSide;

public class Settings {

    private static final double SEEK_TIME_DEF = 100; // milliseconds

    public final BooleanProperty visibleTab = new SimpleBooleanProperty();

    public final ObjectProperty<Side> tabSide = new SimpleObjectProperty<>(Side.BOTTOM);

    private final ObservableBooleanValue tabLeftRight = tabSide.isEqualTo(Side.LEFT).or(
            tabSide.isEqualTo(Side.RIGHT));

    public final DoubleProperty tabHeight = new SimpleDoubleProperty(24);

    public final DoubleBinding tabSpaceHeight = tabHeight.add(7);

    public final ObservableNumberValue tabMarginWidth = Bindings.when(tabLeftRight)
            .then(tabSpaceHeight).otherwise(0);

    public final ObservableNumberValue tabMarginHeight = Bindings.when(tabLeftRight).then(0)
            .otherwise(tabSpaceHeight);

    public final BooleanProperty visibleVideoInfo = new SimpleBooleanProperty();

    public final ObjectProperty<InfoSide> videoInfoSide = new SimpleObjectProperty<>();

    public final DoubleProperty videoInfoMinWidth = new SimpleDoubleProperty();

    public final DoubleProperty videoInfoMinHeight = new SimpleDoubleProperty();

    public final BooleanProperty timesVolumesHorizontal = new SimpleBooleanProperty();

    public final BooleanProperty visibleSpectrums = new SimpleBooleanProperty();

    public final ObjectProperty<NextMode> nextMode = new SimpleObjectProperty<>(NextMode.OTHER);

    public final ReadOnlyObjectProperty<Boolean> direction = new SimpleObjectProperty<>(
            Boolean.TRUE);

    public final DoubleProperty volume = new SimpleDoubleProperty();

    public final BooleanProperty mute = new SimpleBooleanProperty();

    public final ReadOnlyLongProperty loadMediaStepMilliseconds = new SimpleLongProperty(500);

    public final ReadOnlyIntegerProperty skipOnError = new SimpleIntegerProperty(3);

    private Boolean toggleVisibleTab() {
        Boolean ret = visibleTab.getValue();
        visibleTab.set(!ret.booleanValue());
        return ret;
    }

    private Side toggleTabSide() {
        Side ret = tabSide.get();
        if (ret == null) {
            tabSide.setValue(Side.BOTTOM);
        } else {
            switch (ret) {
                case LEFT:
                case RIGHT:
                case TOP:
                    tabSide.setValue(Side.BOTTOM);
                    break;
                case BOTTOM:
                    tabSide.setValue(Side.LEFT);
                    break;
            }
        }
        return ret;
    }

    public void toggleVislbleTabSide() {
        boolean visible = visibleTab.get();
        Side side = tabSide.get();
        if (visible) {
            if (side == Side.LEFT) {
                toggleVisibleTab();
            } else {
                toggleTabSide();
            }
        } else {
            toggleTabSide();
            toggleVisibleTab();
        }
    }

    public Boolean toggleVisibleSpectrums() {
        Boolean ret = visibleSpectrums.getValue();
        visibleSpectrums.set(!ret.booleanValue());
        return ret;
    }

    public Boolean toggleTimesVolumesHorizontal() {
        Boolean ret = timesVolumesHorizontal.getValue();
        timesVolumesHorizontal.set(!ret.booleanValue());
        return ret;
    }

    public boolean thresholdTime(double oldMillis, double newMillis) {
        return SEEK_TIME_DEF < Math.abs(oldMillis - newMillis);
    }

    private Boolean toggleVisibleVideoInfo() {
        Boolean ret = visibleVideoInfo.getValue();
        visibleVideoInfo.set(!ret.booleanValue());
        return ret;
    }

    private InfoSide toggleVideoInfoSide() {
        InfoSide ret = videoInfoSide.getValue();
        if (ret == null) {
            videoInfoSide.setValue(InfoSide.RIGHT_BOTTOM);
        } else {
            switch (ret) {
                case LEFT_TOP:
                    videoInfoSide.setValue(InfoSide.OVERLAY);
                    break;
                case LEFT_BOTTOM:
                case RIGHT_TOP:
                case RIGHT_BOTTOM:
                    videoInfoSide.setValue(InfoSide.LEFT_TOP);
                    break;
                case OVERLAY:
                    videoInfoSide.setValue(InfoSide.RIGHT_BOTTOM);
                    break;
            }
        }
        return ret;
    }

    public void toggleVisibleVideoInfoSide() {
        boolean visible = visibleVideoInfo.get();
        InfoSide side = videoInfoSide.getValue();
        if (visible) {
            if (side == InfoSide.OVERLAY) {
                toggleVisibleVideoInfo();
            } else {
                toggleVideoInfoSide();
            }
        } else {
            toggleVideoInfoSide();
            toggleVisibleVideoInfo();
        }
    }

    public NextMode toggleNextMode() {
        NextMode ret = nextMode.getValue();
        if (ret == null) {
            nextMode.setValue(NextMode.NONE);
        } else {
            switch (ret) {
                case NONE:
                    nextMode.setValue(NextMode.SAME);
                    break;
                case SAME:
                    nextMode.setValue(NextMode.OTHER);
                    break;
                case OTHER:
                    nextMode.setValue(NextMode.NONE);
                    break;
            }
        }
        return ret;
    }

    public void reset() {
        visibleTab.set(false);
        visibleTab.set(true);

        tabSide.setValue(Side.LEFT);
        tabSide.setValue(Side.BOTTOM);

        visibleVideoInfo.set(true);
        visibleVideoInfo.set(false);

        videoInfoSide.setValue(InfoSide.RIGHT_BOTTOM);
        videoInfoSide.setValue(InfoSide.OVERLAY);

        videoInfoMinWidth.set(1);
        videoInfoMinWidth.set(0);
        videoInfoMinHeight.set(1);
        videoInfoMinHeight.set(0);

        timesVolumesHorizontal.set(true);
        timesVolumesHorizontal.set(false);

        visibleSpectrums.set(true);
        visibleSpectrums.set(false);

        nextMode.setValue(NextMode.NONE);
        nextMode.setValue(NextMode.OTHER);

        // direction.setValue(Boolean.FALSE);
        // direction.setValue(Boolean.TRUE);

        mute.set(true);
        mute.set(false);

        volume.set(0d);
        volume.set(0.25d);
    }

    public void setVideoMode() {
        visibleTab.set(false);

        tabSide.setValue(Side.LEFT);

        visibleVideoInfo.set(true);

        videoInfoSide.setValue(InfoSide.LEFT_BOTTOM);

        videoInfoMinWidth.set(360);
        videoInfoMinHeight.set(0);

        timesVolumesHorizontal.set(true);

        visibleSpectrums.set(false);

        // nextMode.setValue(NextMode.NONE);
        // nextMode.setValue(NextMode.OTHER);

        // direction.setValue(Boolean.FALSE);
        // direction.setValue(Boolean.TRUE);
    }
}
