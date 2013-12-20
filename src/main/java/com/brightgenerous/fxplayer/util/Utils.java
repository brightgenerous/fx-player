package com.brightgenerous.fxplayer.util;

import java.io.File;

public class Utils {

    private Utils() {
    }

    public static File getHomeDirectory() {
        File ret = null;
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File tmp = new File(userHome);
            if (tmp.exists() && tmp.isDirectory() && tmp.canRead()) {
                ret = tmp;
            }
        }
        return ret;
    }
}
