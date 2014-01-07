package com.brightgenerous.fxplayer.service;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.brightgenerous.fxplayer.util.ListUtils;
import com.brightgenerous.fxplayer.util.ListUtils.IConverter;
import com.brightgenerous.fxplayer.util.MyServiceUtils;
import com.brightgenerous.fxplayer.util.UrlResolver;
import com.brightgenerous.fxplayer.util.VideoInfo;
import com.brightgenerous.fxplayer.util.XvideosUtils;
import com.brightgenerous.fxplayer.util.YoutubeUtils;

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

    public static List<MediaInfo> fromFiles(File file, MetaChangeListener metaChangeListener) {
        if (file == null) {
            return null;
        }
        return fromFiles(Arrays.asList(file), metaChangeListener);
    }

    public static List<MediaInfo> fromFiles(List<? extends File> files,
            MetaChangeListener metaChangeListener) {
        if (files == null) {
            return null;
        }
        List<MediaInfo> ret = new ArrayList<>();
        for (File file : files) {
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

    public static List<MediaInfo> fromListFile(File file, MetaChangeListener metaChangeListener,
            LoadDirection loadDirection) {
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
        List<VideoInfo> infos = MyServiceUtils.fromFile(file, parent);
        if (infos != null) {
            ret.addAll(convert(infos, metaChangeListener, loadDirection));
        }
        return ret;
    }

    public static List<MediaInfo> fromURL(String str, MetaChangeListener metaChangeListener,
            LoadDirection loadDirection) {
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
                ret.addAll(convert(infos, metaChangeListener, loadDirection));
            }
        } else if (YoutubeUtils.isVideoUrl(str)) {
            String title = YoutubeUtils.extractTitle(text);
            ret.add(createMediaInfo(str, title, metaChangeListener));
        } else if (XvideosUtils.isVideoUrl(str)) {
            String title = XvideosUtils.extractTitle(text);
            ret.add(createMediaInfo(str, title, metaChangeListener));
        } else {
            List<VideoInfo> infos = MyServiceUtils.fromServer(text, serverPath, dirPath);
            if (infos != null) {
                ret.addAll(convert(infos, metaChangeListener, loadDirection));
            }
        }
        return ret;
    }

    private static List<MediaInfo> convert(List<VideoInfo> infos,
            MetaChangeListener metaChangeListener, LoadDirection loadDirection) {
        List<MediaInfo> ret = null;
        if (infos != null) {
            if (loadDirection == null) {
                loadDirection = LoadDirection.ALTERNATELY;
            }
            switch (loadDirection) {
                case FORWARD:
                    ret = ListUtils.converts(infos, new InfoConverter(metaChangeListener));
                    break;
                case BACK:
                    ret = ListUtils.convertsReversely(infos, new InfoConverter(metaChangeListener));
                    break;
                case ALTERNATELY:
                    ret = ListUtils.convertsAlternately(infos,
                            new InfoConverter(metaChangeListener));
                    break;
            }
        }
        return ret;
    }

    private static MediaInfo createMediaInfo(VideoInfo info, MetaChangeListener metaChangeListener) {
        return createMediaInfo(info.getUrl(), info.getTitle(), metaChangeListener);
    }

    private static MediaInfo createMediaInfo(String url, String desc,
            MetaChangeListener metaChangeListener) {
        desc = ((desc == null) || desc.isEmpty()) ? url : desc;
        return factory.create(url, desc, metaChangeListener);
    }

    private static class InfoConverter implements IConverter<MediaInfo, VideoInfo> {

        private final MetaChangeListener metaChangeListener;

        public InfoConverter(MetaChangeListener metaChangeListener) {
            this.metaChangeListener = metaChangeListener;
        }

        @Override
        public MediaInfo convert(VideoInfo obj) {
            return createMediaInfo(obj, metaChangeListener);
        }
    }
}
