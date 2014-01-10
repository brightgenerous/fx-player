package com.brightgenerous.fxplayer.url;

interface ExceptionHandler<T extends Throwable> {

    void handle(T ex);
}
