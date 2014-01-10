package com.brightgenerous.fxplayer.media;

import java.util.Comparator;

public interface IMediaSource {

    Comparator<IMediaSource> UrlComparator = new Comparator<IMediaSource>() {

        @Override
        public int compare(IMediaSource arg0, IMediaSource arg1) {
            if (arg0 == arg1) {
                return 0;
            }
            if (arg0 == null) {
                return 1;
            }
            if (arg1 == null) {
                return -1;
            }
            String url0 = arg0.getUrl();
            String url1 = arg1.getUrl();
            if (url0 == url1) {
                return 0;
            }
            if (url0 == null) {
                return 1;
            }
            if (url1 == null) {
                return -1;
            }
            return url0.compareTo(url1);
        }
    };

    String getUrl();

    String getFileUrl();

    String getDescription();

    void requestResolve(boolean force);

    boolean enablePreLoad();
}
