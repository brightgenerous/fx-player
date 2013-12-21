package com.brightgenerous.fxplayer.media;

public interface IMediaSource {

    String getUrl();

    String getFileUrl();

    String getDescription();

    void requestResolve(boolean force);
}
