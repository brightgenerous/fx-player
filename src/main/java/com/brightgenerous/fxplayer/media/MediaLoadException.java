package com.brightgenerous.fxplayer.media;

public class MediaLoadException extends Exception {

    private static final long serialVersionUID = 4792534199222414498L;

    public MediaLoadException(String message) {
        super(message);
    }

    public MediaLoadException(Throwable cause) {
        super(cause);
    }

    public MediaLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
