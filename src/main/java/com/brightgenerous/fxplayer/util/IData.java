package com.brightgenerous.fxplayer.util;

public interface IData<T> {

    T get();

    void request(boolean force);

    void release();

    void cancel();

    boolean enablePreLoad();
}
