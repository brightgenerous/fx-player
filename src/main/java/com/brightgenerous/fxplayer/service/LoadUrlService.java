package com.brightgenerous.fxplayer.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaInfo.MetaChangeListener;
import com.brightgenerous.fxplayer.util.MyServiceUtils;
import com.brightgenerous.fxplayer.util.YoutubeUtils;

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

    private static final Pattern youtubePattern = Pattern.compile("^\\s*yt\\s*=\\s*(.*)\\s*$");

    private static final Pattern myServicePattern = Pattern
            .compile("^\\s*(?:pl|)\\s*=\\s*(.*)\\s*$");

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
                            String word = matcher.group(1);
                            if (!word.isEmpty()) {
                                url = YoutubeUtils.getQueryUrl(word);
                                text = url;
                            }
                            break parse;
                        }
                    }
                    {
                        Matcher matcher = myServicePattern.matcher(url);
                        if (matcher.find()) {
                            String key = matcher.group(1);
                            if (!key.isEmpty()) {
                                url = MyServiceUtils.getQueryUrl(key);
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
