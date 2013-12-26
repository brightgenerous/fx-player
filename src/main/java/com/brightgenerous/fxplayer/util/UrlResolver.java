package com.brightgenerous.fxplayer.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class UrlResolver {

    private static final UrlResolver instance = new UrlResolver();

    private final ExecutorService youtubeES = Executors.newFixedThreadPool(1);

    private final ExecutorService xvideosES = Executors.newFixedThreadPool(1);

    private UrlResolver() {
    }

    public static UrlResolver get() {
        return instance;
    }

    public IData<String> getFileUrl(String url) {
        IData<String> ret = null;
        if (url == null) {
            ret = new DirectUrl(url);
        } else if (YoutubeUtils.isVideoUrl(url)) {
            ret = new YoutubeUrl(url);
        } else if (XvideosUtils.isVideoUrl(url)) {
            ret = new XvideosUrl(url);
        } else {
            ret = new DirectUrl(url);
        }
        return ret;
    }

    private class DirectUrl implements IData<String> {

        private final String url;

        DirectUrl(String url) {
            this.url = url;
        }

        @Override
        public String get() {
            return url;
        }

        @Override
        public void request(boolean force) {
        }

        @Override
        public void cancel() {
        }
    }

    private class YoutubeUrl implements IData<String> {

        private final String url;

        private volatile FutureTask<String> future;

        private final Object lock = new Object();

        YoutubeUrl(String url) {
            this.url = url;
            request(false);
        }

        @Override
        public String get() {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
            }
            return null;
        }

        @Override
        public void request(boolean force) {
            if (!force && (future != null)) {
                return;
            }
            synchronized (lock) {
                if (!force && (future != null)) {
                    return;
                }
                if ((future != null) && !future.isDone()) {
                    return;
                }
                FutureTask<String> ftr = new FutureTask<>(new Callable<String>() {

                    @Override
                    public String call() throws Exception {
                        return YoutubeUtils.extractUrl(url);
                    }
                });
                youtubeES.execute(ftr);
                future = ftr;
            }
        }

        @Override
        public void cancel() {
            future.cancel(true);
        }
    }

    private class XvideosUrl implements IData<String> {

        private final String url;

        private volatile FutureTask<String> future;

        private final Object lock = new Object();

        XvideosUrl(String url) {
            this.url = url;
            request(false);
        }

        @Override
        public String get() {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
            }
            return null;
        }

        @Override
        public void request(boolean force) {
            if (!force && (future != null)) {
                return;
            }
            synchronized (lock) {
                if (!force && (future != null)) {
                    return;
                }
                if ((future != null) && !future.isDone()) {
                    return;
                }
                FutureTask<String> ftr = new FutureTask<>(new Callable<String>() {

                    @Override
                    public String call() throws Exception {
                        return XvideosUtils.extractUrl(url);
                    }
                });
                xvideosES.execute(ftr);
                future = ftr;
            }
        }

        @Override
        public void cancel() {
            future.cancel(true);
        }
    }
}
