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
import com.brightgenerous.fxplayer.service.LoadDirection;

class Settings {

    private static final double SEEK_TIME_DEFF = 100; // milliseconds

    private static final double CURRENT_TIME_DEFF = 100; // milliseconds

    public final BooleanProperty hideHeader = new SimpleBooleanProperty(this, "hideHeader");

    public final BooleanProperty hideFooter = new SimpleBooleanProperty(this, "hideFooter");

    public final BooleanProperty visibleTab = new SimpleBooleanProperty(this, "visibleTab");

    public final ObjectProperty<Side> tabSide = new SimpleObjectProperty<>(this, "tabSide",
            Side.BOTTOM);

    private final ObservableBooleanValue tabLeftRight = tabSide.isEqualTo(Side.LEFT).or(
            tabSide.isEqualTo(Side.RIGHT));

    public final DoubleProperty tabHeight;

    public final DoubleBinding tabSpaceHeight;

    {
        boolean win;
        {
            String os = System.getProperty("os.name");
            win = (os != null) && os.toLowerCase().contains("windows");
        }
        tabHeight = new SimpleDoubleProperty(this, "tabHeight", 24);
        tabSpaceHeight = tabHeight.add(win ? 7 : 10);
    }

    public final ObservableNumberValue tabMarginWidth = Bindings.when(tabLeftRight)
            .then(tabSpaceHeight).otherwise(0);

    public final ObservableNumberValue tabMarginHeight = Bindings.when(tabLeftRight).then(0)
            .otherwise(tabSpaceHeight);

    public final BooleanProperty visibleVideoInfo = new SimpleBooleanProperty(this,
            "visibleVideoInfo");

    public final ObjectProperty<InfoSide> videoInfoSide = new SimpleObjectProperty<>(this,
            "videoInfoSide");

    public final DoubleProperty videoInfoMinWidth = new SimpleDoubleProperty(this,
            "videoInfoMinWidth");

    public final DoubleProperty videoInfoMaxWidth = new SimpleDoubleProperty(this,
            "videoInfoMaxWidth");

    public final DoubleProperty videoInfoMinHeight = new SimpleDoubleProperty(this,
            "videoInfoMinHeight");

    public final DoubleProperty videoInfoMaxHeight = new SimpleDoubleProperty(this,
            "videoInfoMaxHeight");

    public final BooleanProperty visibleSpectrums = new SimpleBooleanProperty(this,
            "visibleSpectrums");

    public final BooleanProperty timesVolumesHorizontal = new SimpleBooleanProperty(this,
            "timesVolumesHorizontal");

    public final ObjectProperty<NextMode> nextMode = new SimpleObjectProperty<>(this, "nextMode",
            NextMode.OTHER);

    public final ObjectProperty<OtherDirection> otherDirection = new SimpleObjectProperty<>(this,
            "otherDirection", OtherDirection.FORWARD);

    public final ReadOnlyObjectProperty<LoadDirection> loadDirection = new SimpleObjectProperty<>(
            this, "loadDirection", LoadDirection.ALTERNATELY);

    public final DoubleProperty volume = new SimpleDoubleProperty(this, "volume");

    public final BooleanProperty mute = new SimpleBooleanProperty(this, "mute");

    public final ReadOnlyLongProperty loadMediaStepMilliseconds = new SimpleLongProperty(this,
            "loadMediaStepMilliseconds", 500);

    public final ReadOnlyIntegerProperty skipOnError = new SimpleIntegerProperty(this,
            "skipOnError", 3);

    public Boolean toggleHideHeader() {
        Boolean ret = hideHeader.getValue();
        hideHeader.set((ret == null) ? true : !ret.booleanValue());
        return ret;
    }

    public Boolean toggleHideFooter() {
        Boolean ret = hideFooter.getValue();
        hideFooter.set((ret == null) ? true : !ret.booleanValue());
        return ret;
    }

    private Boolean toggleVisibleTab() {
        Boolean ret = visibleTab.getValue();
        visibleTab.set((ret == null) ? true : !ret.booleanValue());
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

    public void setVideoInfoWidth(double width) {
        videoInfoMinHeight.set(0);
        videoInfoMaxHeight.set(Double.NaN);
        if (Double.isNaN(width)) {
            videoInfoMinWidth.set(0);
            videoInfoMaxWidth.set(Double.NaN);
        } else {
            videoInfoMinWidth.set(Math.max(width, 0));
            videoInfoMaxWidth.set(videoInfoMinWidth.get());
        }
    }

    public void setVideoInfoWidthPlus(double width) {
        videoInfoMinHeight.set(0);
        videoInfoMaxHeight.set(Double.NaN);
        if (Double.isNaN(width)) {
            videoInfoMinWidth.set(0);
            videoInfoMaxWidth.set(Double.NaN);
        } else {
            double current = videoInfoMinWidth.get();
            if (Double.isNaN(current)) {
                current = 0;
            }
            videoInfoMinWidth.set(Math.max(current + width, 0));
            videoInfoMaxWidth.set(videoInfoMinWidth.get());
        }
    }

    public void setVideoInfoHeight(double height) {
        videoInfoMinWidth.set(0);
        videoInfoMaxWidth.set(Double.NaN);
        if (Double.isNaN(height)) {
            videoInfoMinHeight.set(0);
            videoInfoMaxHeight.set(Double.NaN);
        } else {
            videoInfoMinHeight.set(Math.max(height, 0));
            videoInfoMaxHeight.set(videoInfoMinHeight.get());
        }
    }

    public void setVideoInfoHeightPlus(double height) {
        videoInfoMinWidth.set(0);
        videoInfoMaxWidth.set(Double.NaN);
        if (Double.isNaN(height)) {
            videoInfoMinHeight.set(0);
            videoInfoMaxHeight.set(Double.NaN);
        } else {
            double current = videoInfoMinHeight.get();
            if (Double.isNaN(current)) {
                current = 0;
            }
            videoInfoMinHeight.set(Math.max(current + height, 0));
            videoInfoMaxHeight.set(videoInfoMinHeight.get());
        }
    }

    public Boolean toggleVisibleSpectrums() {
        Boolean ret = visibleSpectrums.getValue();
        visibleSpectrums.set((ret == null) ? true : !ret.booleanValue());
        return ret;
    }

    public Boolean toggleTimesVolumesHorizontal() {
        Boolean ret = timesVolumesHorizontal.getValue();
        timesVolumesHorizontal.set((ret == null) ? true : !ret.booleanValue());
        return ret;
    }

    public boolean thresholdTimeSeek(double oldMillis, double newMillis) {
        return SEEK_TIME_DEFF < Math.abs(oldMillis - newMillis);
    }

    public boolean thresholdTimeCurrent(double oldMillis, double newMillis) {
        return CURRENT_TIME_DEFF < Math.abs(oldMillis - newMillis);
    }

    private Boolean toggleVisibleVideoInfo() {
        Boolean ret = visibleVideoInfo.getValue();
        visibleVideoInfo.set((ret == null) ? true : !ret.booleanValue());
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

    public OtherDirection toggleOtherDirection() {
        OtherDirection ret = otherDirection.getValue();
        if (ret == null) {
            otherDirection.setValue(OtherDirection.FORWARD);
        } else {
            switch (ret) {
                case FORWARD:
                    otherDirection.setValue(OtherDirection.BACK);
                    break;
                case BACK:
                    otherDirection.setValue(OtherDirection.FORWARD);
                    break;
            }
        }
        return ret;
    }

    public void reset() {
        hideHeader.set(true);
        hideHeader.set(false);
        hideFooter.set(true);
        hideFooter.set(false);

        visibleTab.set(false);
        visibleTab.set(true);

        tabSide.setValue(Side.LEFT);
        tabSide.setValue(Side.BOTTOM);

        visibleVideoInfo.set(true);
        visibleVideoInfo.set(false);

        videoInfoSide.setValue(InfoSide.RIGHT_BOTTOM);
        videoInfoSide.setValue(InfoSide.OVERLAY);

        setVideoInfoWidth(0);
        setVideoInfoWidth(Double.NaN);

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
        hideHeader.set(true);
        hideFooter.set(false);

        visibleTab.set(false);

        tabSide.setValue(Side.LEFT);

        visibleVideoInfo.set(true);

        videoInfoSide.setValue(InfoSide.LEFT_TOP);

        setVideoInfoWidth(360);

        timesVolumesHorizontal.set(true);

        visibleSpectrums.set(false);

        // nextMode.setValue(NextMode.NONE);
        // nextMode.setValue(NextMode.OTHER);

        // direction.setValue(Boolean.FALSE);
        // direction.setValue(Boolean.TRUE);
    }
}
