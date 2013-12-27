package com.brightgenerous.fxplayer.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.media.Media;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.media.MediaInfoFactory;
import com.brightgenerous.fxplayer.util.HttpUtils;
import com.brightgenerous.fxplayer.util.IData;
import com.brightgenerous.fxplayer.util.UrlResolver;
import com.brightgenerous.fxplayer.util.XvideosUtils;
import com.brightgenerous.fxplayer.util.YoutubeUtils;
import com.brightgenerous.fxplayer.util.YoutubeUtils.VideoInfo;

class MediaInfoLoader {

    private static final MediaInfoFactory factory;
    static {
        Map<String, SoftReference<Media>> mediaCache = new LimitedCache<>();
        Map<String, SoftReference<IData<String>>> resolveCache = new LimitedCache<>();
        factory = new MediaInfoFactory(UrlResolver.get(), mediaCache, resolveCache);
    }

    private static class LimitedCache<K, V> extends ConcurrentHashMap<K, SoftReference<V>> {

        private static final long serialVersionUID = 4020277383498994461L;

        @Override
        public SoftReference<V> put(K key, SoftReference<V> value) {
            truncate();
            return super.put(key, value);
        }

        @Override
        public SoftReference<V> putIfAbsent(K key, SoftReference<V> value) {
            truncate();
            return super.putIfAbsent(key, value);
        }

        @Override
        public void putAll(Map<? extends K, ? extends SoftReference<V>> m) {
            truncate();
            super.putAll(m);
        }

        private volatile long last = Long.MIN_VALUE;

        private final Object lockTime = new Object();

        private void truncate() {
            if (System.currentTimeMillis() < (last + 60_000)) {
                return;
            }
            synchronized (lockTime) {
                long time = System.currentTimeMillis();
                if (time < (last + 60_000)) {
                    return;
                }
                last = time;
            }
            Set<K> dels = new HashSet<>();
            for (Entry<K, SoftReference<V>> e : entrySet()) {
                SoftReference<V> v = e.getValue();
                if ((v == null) || (v.get() == null)) {
                    dels.add(e.getKey());
                }
            }
            for (K del : dels) {
                remove(del);
            }
        }
    }

    private MediaInfoLoader() {
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
                    ret.add(factory.create(path, file.getName(), metaChangeListener));
                }
            }
        }
        Collections.sort(ret, new Comparator<MediaInfo>() {

            @Override
            public int compare(MediaInfo o1, MediaInfo o2) {
                return o1.getSource().getUrl().compareTo(o2.getSource().getUrl());
            }
        });
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
                    {
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
                    ret.add(factory.create(path, desc, metaChangeListener));
                }
            }
        } catch (IOException e) {
        }
        return ret;
    }

    public static List<MediaInfo> fromURL(String str, MetaChangeListener metaChangeListener) {
        if (str == null) {
            return null;
        }

        URL url;
        try {
            url = new URL(str);
        } catch (MalformedURLException e) {
            return null;
        }

        String proto = url.getProtocol();
        boolean isHttp = proto.equals("http");
        boolean isHttps = proto.equals("https");
        if (!isHttp && !isHttps) {
            return null;
        }

        if (isHttps) {
            // unsupported...
            return null;
        }

        String host = url.getHost();
        if ((host == null) || host.isEmpty()) {
            return null;
        }

        String serverPath = proto + "://" + host;
        String dirPath;
        {
            String path = url.getPath();
            if ((path == null) || path.isEmpty()) {
                dirPath = serverPath + "/";
            } else {
                int idx = path.lastIndexOf("/");
                if (idx < 0) {
                    dirPath = serverPath + "/";
                } else {
                    dirPath = serverPath + path.substring(0, idx) + "/";
                }
            }
        }

        String text = null;
        try {
            text = HttpUtils.execGet(str, Charset.forName("UTF-8"));
        } catch (IOException e) {
        }
        if (text == null) {
            return null;
        }

        List<MediaInfo> ret = new ArrayList<>();
        if (YoutubeUtils.isPlaylistUrl(str)) {
            List<VideoInfo> infos = YoutubeUtils.parsePlaylist(text);
            if (infos != null) {
                for (VideoInfo info : infos) {
                    String desc = (info.getTitle() == null) ? info.getUrl() : info.getTitle();
                    ret.add(factory.create(info.getUrl(), desc, metaChangeListener));
                }
            }
        } else if (XvideosUtils.isVideoUrl(str)) {
            String title = XvideosUtils.extractTitle(text);
            String desc = ((title == null) || title.isEmpty()) ? str : title;
            ret.add(factory.create(str, desc, metaChangeListener));
        } else {
            try (BufferedReader br = new BufferedReader(new StringReader(text))) {
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
                        if (tmp.startsWith("http://") || tmp.startsWith("https://")) {
                            path = tmp;
                        } else {
                            if (tmp.startsWith("/")) {
                                path = serverPath + tmp;
                            } else {
                                path = dirPath
                                        + URLEncoder.encode(tmp, "UTF-8").replace("+", "%20");
                            }
                        }
                    }
                    {
                        String desc = (strs[1] == null) ? strs[0] : strs[1];
                        ret.add(factory.create(path, desc, metaChangeListener));
                    }
                }
            } catch (IOException e) {
            }
        }
        return ret;
    }

    private static String[] lineToStrs(String line) {
        String[] ret = new String[2];
        if ((line != null)) {
            String[] strs = line.trim().split("\t+");
            ret[0] = strs[0].trim();
            for (int i = 1; i < strs.length; i++) {
                String str = strs[i].trim();
                if (!str.isEmpty()) {
                    if (!str.startsWith("#")) {
                        ret[1] = str;
                    }
                    break;
                }
            }
        }
        return ret;
    }
}
