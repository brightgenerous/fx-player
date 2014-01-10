package com.brightgenerous.fxplayer.util;

public interface IData<T> {

    T get();

    void request(boolean force);

    void cancel();

    boolean enablePreLoad();
}
