package com.brightgenerous.fxplayer.application.playlist;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.geometry.Side;

import com.brightgenerous.fxplayer.application.playlist.VideoPane.InfoSide;

public class Settings {

    public static final double DEFAULT_VOLUME = 0.25d;

    private static final double SEEK_TIME_DEF = 100; // milliseconds

    public final ObjectProperty<Side> tabSide = new SimpleObjectProperty<>(Side.BOTTOM);

    public final ObservableBooleanValue tabLeftRight = tabSide.isEqualTo(Side.LEFT).or(
            tabSide.isEqualTo(Side.RIGHT));

    public final ObservableNumberValue tabMarginWidth = Bindings.when(tabLeftRight).then(32)
            .otherwise(0);

    public final ObservableNumberValue tabMarginHeight = Bindings.when(tabLeftRight).then(0)
            .otherwise(32);

    public final BooleanProperty visibleVideoInfo = new SimpleBooleanProperty();

    public final ObjectProperty<InfoSide> videoInfoSide = new SimpleObjectProperty<>();

    public final BooleanProperty timesVolumesHorizontal = new SimpleBooleanProperty();

    public final BooleanProperty visibleSpectrums = new SimpleBooleanProperty();

    public Side toggleTabSide() {
        Side ret = tabSide.get();
        if (tabLeftRight.getValue().booleanValue()) {
            tabSide.set(Side.BOTTOM);
        } else {
            tabSide.set(Side.LEFT);
        }
        return ret;
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

    public Boolean toggleVisibleVideoInfo() {
        Boolean ret = visibleVideoInfo.getValue();
        visibleVideoInfo.set(!ret.booleanValue());
        return ret;
    }

    public InfoSide toggleVideoInfoSide() {
        InfoSide ret = videoInfoSide.getValue();
        if (ret == null) {
            videoInfoSide.setValue(InfoSide.RIGHT_BOTTOM);
        } else {
            switch (ret) {
                case LEFT_TOP:
                    videoInfoSide.setValue(InfoSide.RIGHT_BOTTOM);
                    break;
                case RIGHT_BOTTOM:
                    videoInfoSide.setValue(InfoSide.LEFT_TOP);
                    break;
            }
        }
        return ret;
    }

    public void toggleVisibleVideoInfoSide() {
        boolean visible = visibleVideoInfo.get();
        InfoSide side = videoInfoSide.getValue();
        if (!visible) {
            toggleVisibleVideoInfo();
        } else {
            toggleVideoInfoSide();
            if ((side != null) && side.equals(InfoSide.LEFT_TOP)) {
                toggleVisibleVideoInfo();
            }
        }
    }
}
