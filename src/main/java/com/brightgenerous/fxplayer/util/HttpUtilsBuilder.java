package com.brightgenerous.fxplayer.util;

import java.nio.charset.Charset;

public class HttpUtilsBuilder {

    private String userAgent;

    private String contentType;

    private Charset encode;

    private boolean selfSigned;

    private boolean syncCookie;

    private HttpUtilsBuilder() {
    }

    public static HttpUtilsBuilder create() {
        return new HttpUtilsBuilder();
    }

    public static HttpUtilsBuilder createDefault() {
        return create().userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0.1)").encode("UTF-8");
    }

    public HttpUtils build() {
        return new HttpUtils(userAgent, contentType, encode, selfSigned, syncCookie);
    }

    public String userAgent() {
        return userAgent;
    }

    public HttpUtilsBuilder userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public String contentType() {
        return contentType;
    }

    public HttpUtilsBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Charset encode() {
        return encode;
    }

    public HttpUtilsBuilder encode(Charset encode) {
        this.encode = encode;
        return this;
    }

    public HttpUtilsBuilder encode(String encode) {
        return encode(Charset.forName(encode));
    }

    public boolean selfSigned() {
        return selfSigned;
    }

    public HttpUtilsBuilder selfSigned(boolean selfSigned) {
        this.selfSigned = selfSigned;
        return this;
    }

    public boolean syncCookie() {
        return syncCookie;
    }

    public HttpUtilsBuilder syncCookie(boolean syncCookie) {
        this.syncCookie = syncCookie;
        return this;
    }
}
