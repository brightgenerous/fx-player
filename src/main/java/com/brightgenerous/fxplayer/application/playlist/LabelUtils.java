package com.brightgenerous.fxplayer.application.playlist;

class LabelUtils {

    private LabelUtils() {
    }

    public static String milliSecToTime(double millis) {
        int sec = (int) (millis / 1000);
        return String.format("%3d:%02d", Integer.valueOf(sec / 60), Integer.valueOf(sec % 60));
    }

    public static String toVolume(double vol) {
        return String.format("%3d%%", Integer.valueOf((int) (vol * 100)));
    }
}
