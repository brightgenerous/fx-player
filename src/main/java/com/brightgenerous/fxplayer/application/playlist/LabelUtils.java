package com.brightgenerous.fxplayer.application.playlist;

class LabelUtils {

    private LabelUtils() {
    }

    public static String milliSecToTime(double millis) {
        int sec = (int) (millis / 1000);
        return String.format("%3d:%02d", Integer.valueOf(sec / 60), Integer.valueOf(sec % 60));
    }

    public static String milliSecsToTime(double current, double max, double buffer) {
        int currentSec = (int) (current / 1000);
        int maxSec = (int) (max / 1000);
        int bufferSec = (int) (buffer / 1000);
        return String.format("%3d:%02d /%3d:%02d [%3d:%02d]", Integer.valueOf(currentSec / 60),
                Integer.valueOf(currentSec % 60), Integer.valueOf(maxSec / 60),
                Integer.valueOf(maxSec % 60), Integer.valueOf(bufferSec / 60),
                Integer.valueOf(bufferSec % 60));
    }

    public static String toVolume(double vol) {
        return String.format("%3d%%", Integer.valueOf((int) (vol * 100)));
    }

    public static String toTabLabel(String str) {
        return str;
    }
}
