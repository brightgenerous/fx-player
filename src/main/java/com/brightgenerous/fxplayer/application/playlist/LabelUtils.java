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
        int bufPer = Math.min((int) ((buffer * 100) / max), 100);
        return String.format("%3d:%02d /%3d:%02d [%3d%%]", Integer.valueOf(currentSec / 60),
                Integer.valueOf(currentSec % 60), Integer.valueOf(maxSec / 60),
                Integer.valueOf(maxSec % 60), Integer.valueOf(bufPer));
    }

    public static String toVolume(double vol) {
        return String.format("%3d", Integer.valueOf((int) (vol * 100)));
    }

    public static String toTabLabel(String str) {
        if (str == null) {
            return null;
        }
        String ret = cutoff(str, 45);
        if ((ret != null) && !ret.equals(str)) {
            ret += "…";
        }
        return ret;
    }

    private static String cutoff(String str, int width) {
        if (str == null) {
            return null;
        }
        if ((width < 1) || str.isEmpty()) {
            return "";
        }
        int length = str.codePointCount(0, str.length());
        if (length <= width) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; (count < width) && (i < str.length()); i++) {
            char c = str.charAt(i);
            sb.append(c);
            if (((i + 1) < str.length()) && Character.isSurrogatePair(c, str.charAt(i + 1))) {
                sb.append(str.charAt(++i));
            }
            count++;
        }
        return sb.toString();
    }
}
