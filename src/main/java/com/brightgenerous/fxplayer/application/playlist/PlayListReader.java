package com.brightgenerous.fxplayer.application.playlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.brightgenerous.fxplayer.application.playlist.MediaInfo.MetaChangeListener;

class PlayListReader {

    private PlayListReader() {
    }

    public static List<MediaInfo> fromDirectory(File dir, MetaChangeListener metaChangeListener) {
        if ((dir == null) || !dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            return null;
        }
        List<MediaInfo> ret = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.exists() && file.isFile() && file.canRead()) {
                String path = null;
                try {
                    path = file.toURI().toURL().toString();
                } catch (MalformedURLException e) {
                }
                if (path != null) {
                    ret.add(new MediaInfo(new MediaSource(path, file.getName()), metaChangeListener));
                }
            }
        }
        return ret;
    }

    public static List<MediaInfo> fromFile(File file, MetaChangeListener metaChangeListener) {
        if ((file == null) || !file.exists() || !file.canRead()) {
            return null;
        }

        if (1_000_000 < file.length()) {
            // too large
            return null;
        }

        File parent = file.getParentFile();
        if ((parent != null) && !parent.isDirectory()) {
            parent = null;
        }

        List<MediaInfo> ret = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] strs = lineToStrs(line);
                if ((strs == null) || (strs[0] == null)) {
                    continue;
                }
                String path = null;
                {
                    String tmp = strs[0];
                    if (path == null) {
                        if (parent != null) {
                            File f = new File(parent, tmp);
                            if (f.exists()) {
                                try {
                                    path = f.toURI().toURL().toString();
                                } catch (MalformedURLException e) {
                                }
                            }
                        }
                    }
                    if (path == null) {
                        File f = new File(tmp);
                        if (f.exists()) {
                            try {
                                path = f.toURI().toURL().toString();
                            } catch (MalformedURLException e) {
                            }
                        }
                    }
                    if (path == null) {
                        try {
                            path = new URL(tmp).toString();
                        } catch (MalformedURLException e) {
                        }
                    }
                }
                if (path != null) {
                    String desc = (strs[1] == null) ? strs[0] : strs[1];
                    ret.add(new MediaInfo(new MediaSource(path, desc), metaChangeListener));
                }
            }
        } catch (IOException e) {
        }
        return ret;
    }

    public static List<MediaInfo> fromURL(String str, MetaChangeListener metaChangeListener) {
        if ((str == null) || !str.startsWith("http://")) {
            return null;
        }

        URL url = null;
        try {
            url = new URL(str);
        } catch (MalformedURLException e) {
        }
        if (url == null) {
            return null;
        }

        String serverPath;
        String dirPath;
        {
            int idx = str.indexOf("/", "http://".length());
            if (idx == -1) {
                serverPath = str;
                dirPath = str;
            } else {
                serverPath = str.substring(0, idx);
                dirPath = str.substring(0, str.lastIndexOf("/"));
            }
            if (!dirPath.endsWith("/")) {
                dirPath = dirPath + "/";
            }
        }

        List<MediaInfo> ret = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(url.openStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] strs = lineToStrs(line);
                if ((strs == null) || (strs[0] == null)) {
                    continue;
                }
                String path = null;
                {
                    String tmp = strs[0];
                    if (!tmp.startsWith("http://")) {
                        if (tmp.startsWith("/")) {
                            path = serverPath + tmp;
                        } else {
                            path = dirPath + URLEncoder.encode(tmp, "UTF-8").replace("+", "%20");
                        }
                    } else {
                        path = tmp;
                    }
                }
                if (path != null) {
                    String desc = (strs[1] == null) ? strs[0] : strs[1];
                    ret.add(new MediaInfo(new MediaSource(path, desc), metaChangeListener));
                }
            }
        } catch (IOException e) {
        }
        return ret;
    }

    private static String[] lineToStrs(String line) {
        String[] ret = new String[2];
        if ((line != null)) {
            String[] strs = line.trim().split("\t");
            ret[0] = strs[0].trim();
            for (int i = 1; i < strs.length; i++) {
                String str = strs[i];
                if (str != null) {
                    str = str.trim();
                    if (!str.isEmpty()) {
                        if (!str.startsWith("#")) {
                            ret[1] = str;
                        }
                        break;
                    }
                }
            }
        }
        return ret;
    }
}
