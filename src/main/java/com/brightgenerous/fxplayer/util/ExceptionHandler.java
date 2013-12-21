package com.brightgenerous.fxplayer.util;

interface ExceptionHandler<T extends Throwable> {

    void handle(T ex);
}
