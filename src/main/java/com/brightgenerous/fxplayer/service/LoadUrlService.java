package com.brightgenerous.fxplayer.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.url.UrlDispathcer;

public class LoadUrlService extends Service<List<? extends MediaInfo>> {

    public static interface ICallback {

        void callback(String text, String url, List<? extends MediaInfo> infos);
    }

    private final ObservableValue<String> textProperty;

    private final MetaChangeListener metaChangeListener;

    private final ObservableValue<LoadDirection> loadDirectionProperty;

    private final ICallback callback;

    public LoadUrlService(ObservableValue<String> textProperty,
            MetaChangeListener metaChangeListener,
            ObservableValue<LoadDirection> loadDirectionProperty, ICallback callback) {
        this.textProperty = textProperty;
        this.metaChangeListener = metaChangeListener;
        this.loadDirectionProperty = loadDirectionProperty;
        this.callback = callback;
    }

    private static final Pattern youtubePattern = Pattern.compile("^\\s*yt\\s*=\\s*(.*)$");

    private static final Pattern niconicoPattern = Pattern.compile("^\\s*nc\\s*=\\s*(.*)$");

    private static final Pattern myServicePattern = Pattern.compile("^\\s*(?:pl|)\\s*=\\s*(.*)$");

    private static class Query {

        String word;

        Integer page;

        Query(String word, Integer page) {
            this.word = word;
            this.page = page;
        }
    }

    private static Query getQuery(String str) {
        return getQuery(str, null);
    }

    private static Query getQuery(String str, int inc) {
        return getQuery(str, Integer.valueOf(inc));
    }

    private static Query getQuery(String str, Integer inc) {
        String[] tmps = str.split(",");
        String word = tmps[0].trim();
        Integer page = null;
        for (int i = 1; i < tmps.length; i++) {
            String tmp = tmps[i].trim();
            if (tmp.startsWith("page")) {
                tmp = tmp.replaceAll("page\\s*=\\s*", "");
                try {
                    page = Integer.valueOf(tmp);
                    break;
                } catch (NumberFormatException e) {
                }
            }
        }
        if (inc != null) {
            int p = (page == null) ? (1 + inc.intValue()) : (page.intValue() + inc.intValue());
            page = Integer.valueOf(p);
        }
        return new Query(word, page);
    }

    public static String getQueryPageUrl(String url, int inc) {
        String ret = null;
        parse: {
            {
                Matcher matcher = youtubePattern.matcher(url);
                if (matcher.find()) {
                    String str = matcher.group(1);
                    if (!str.isEmpty()) {
                        Query query = getQuery(str, inc);
                        ret = "yt=" + query.word + ",page=" + query.page;
                    }
                    break parse;
                }
            }
            {
                Matcher matcher = niconicoPattern.matcher(url);
                if (matcher.find()) {
                    String str = matcher.group(1);
                    if (!str.isEmpty()) {
                        Query query = getQuery(str, inc);
                        ret = "nc=" + query.word + ",page=" + query.page;
                    }
                    break parse;
                }
            }
            {
                Matcher matcher = myServicePattern.matcher(url);
                if (matcher.find()) {
                    String str = matcher.group(1);
                    if (!str.isEmpty()) {
                        Query query = getQuery(str, inc);
                        ret = "=" + query.word + ",page=" + query.page;
                    }
                    break parse;
                }
            }
        }
        if (ret != null) {
            return ret;
        }
        return UrlDispathcer.getQueryPageUrl(url, inc);
    }

    @Override
    protected Task<List<? extends MediaInfo>> createTask() {
        return new Task<List<? extends MediaInfo>>() {

            @Override
            protected List<MediaInfo> call() throws Exception {
                if (isCancelled()) {
                    return null;
                }

                String text = textProperty.getValue();
                String url = text;
                if (url == null) {
                    return null;
                }
                url = url.trim();
                if (url.isEmpty()) {
                    return null;
                }

                parse: {
                    {
                        Matcher matcher = youtubePattern.matcher(url);
                        if (matcher.find()) {
                            String str = matcher.group(1);
                            if (!str.isEmpty()) {
                                Query query = getQuery(str);
                                url = UrlDispathcer.createQueryUrl(query.word, query.page,
                                        UrlDispathcer.Service.YOUTUBE);
                            }
                            break parse;
                        }
                    }
                    {
                        Matcher matcher = niconicoPattern.matcher(url);
                        if (matcher.find()) {
                            String str = matcher.group(1);
                            if (!str.isEmpty()) {
                                Query query = getQuery(str);
                                url = UrlDispathcer.createQueryUrl(query.word, query.page,
                                        UrlDispathcer.Service.NICONICO);
                            }
                            break parse;
                        }
                    }
                    {
                        Matcher matcher = myServicePattern.matcher(url);
                        if (matcher.find()) {
                            String str = matcher.group(1);
                            if (!str.isEmpty()) {
                                Query query = getQuery(str);
                                url = UrlDispathcer.createQueryUrl(query.word, query.page,
                                        UrlDispathcer.Service.MYSERVICE);
                            }
                            break parse;
                        }
                    }
                }
                List<MediaInfo> infos = MediaInfoLoader.fromURL(url, metaChangeListener,
                        loadDirectionProperty.getValue());

                if (isCancelled()) {
                    return null;
                }

                callback.callback(text, url, infos);
                return infos;
            }
        };
    }
}
