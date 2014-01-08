package com.brightgenerous.fxplayer.util;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class HttpUtils {

    private static final ReadWriteLock cookieManagerLock = new ReentrantReadWriteLock();

    static {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
    }

    private final String userAgent;

    private final String contentType;

    private final Charset encode;

    private final boolean selfSigned;

    private final boolean syncCookie;

    HttpUtils(String userAgent, String contentType, Charset encode, boolean selfSigned,
            boolean syncCookie) {
        this.userAgent = userAgent;
        this.contentType = contentType;
        this.encode = encode;
        this.selfSigned = selfSigned;
        this.syncCookie = syncCookie;
    }

    public String execGet(String url) throws IOException {
        return exec(false, url, userAgent, null, contentType, encode, selfSigned, syncCookie);
    }

    public String execPost(String url, String body) throws IOException {
        return exec(true, url, userAgent, body, contentType, encode, selfSigned, syncCookie);
    }

    private static String exec(boolean post, String url, String userAgent, String body,
            String contentType, final Charset encode, boolean selfSigned, boolean syncCookie)
            throws IOException {

        // context
        HttpClientContext context = HttpClientContext.create();

        // http client builder
        HttpClientBuilder builder = HttpClientBuilder.create();
        {
            // ignoring SSL certificate (for trusting self-signed certificates)
            if (selfSigned) {
                SSLConnectionSocketFactory sslsf;
                try {
                    SSLContextBuilder scb = new SSLContextBuilder();
                    scb.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                    sslsf = new SSLConnectionSocketFactory(scb.build());
                } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    // Unexpected.
                    throw new RuntimeException(e);
                }
                builder.setSSLSocketFactory(sslsf);
            }

            // redirect for POST
            {
                builder.setRedirectStrategy(new RedirectStrategy() {

                    private final RedirectStrategy deleg = DefaultRedirectStrategy.INSTANCE;

                    @Override
                    public boolean isRedirected(HttpRequest request, HttpResponse response,
                            HttpContext context) throws ProtocolException {
                        boolean isRedirected = deleg.isRedirected(request, response, context);
                        if (!isRedirected) {
                            if (request instanceof HttpUriRequest) {
                                String method = ((HttpUriRequest) request).getMethod();
                                if ((method != null) && !method.equalsIgnoreCase("post")) {
                                    return isRedirected;
                                }
                            }
                            int statusCode = response.getStatusLine().getStatusCode();
                            if ((statusCode == 301) || (statusCode == 302)) {
                                return true;
                            }
                        }
                        return isRedirected;
                    }

                    @Override
                    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response,
                            HttpContext context) throws ProtocolException {
                        return deleg.getRedirect(request, response, context);
                    }
                });
            }
        }

        // cookie
        CookieStore cookieStore = null;
        if (syncCookie) {
            CookieHandler handler = CookieHandler.getDefault();
            if (handler instanceof CookieManager) {
                CookieManager manager = (CookieManager) handler;
                cookieStore = new BasicCookieStore();
                Lock lock = cookieManagerLock.readLock();
                try {
                    lock.lock();
                    for (HttpCookie httpCookie : manager.getCookieStore().getCookies()) {
                        BasicClientCookie cookie = new BasicClientCookie(httpCookie.getName(),
                                httpCookie.getValue());
                        cookie.setComment(httpCookie.getComment());
                        cookie.setDomain(httpCookie.getDomain());
                        cookie.setPath(httpCookie.getPath());
                        cookie.setSecure(httpCookie.getSecure());
                        cookieStore.addCookie(cookie);
                    }
                } finally {
                    lock.unlock();
                }
                builder.setDefaultCookieStore(cookieStore);
            }
        }

        // define request
        HttpUriRequest request;
        if (post) {
            HttpPost req = new HttpPost(url);
            if (userAgent != null) {
                req.setHeader("User-Agent", userAgent);
            }
            if (contentType != null) {
                req.setHeader("Content-Type", contentType);
            }
            if (body != null) {
                req.setEntity(new StringEntity(body, encode));
            }
            request = req;
        } else {
            HttpGet req = new HttpGet(url);
            if (userAgent != null) {
                req.setHeader("User-Agent", userAgent);
            }
            request = req;
        }

        String ret;
        try (CloseableHttpClient httpClient = builder.build()) {
            ret = httpClient.execute(request, new ResponseHandler<String>() {

                @Override
                public String handleResponse(HttpResponse response) throws IOException {
                    switch (response.getStatusLine().getStatusCode()) {
                        case HttpStatus.SC_OK:
                            HttpEntity entity = response.getEntity();
                            Charset charset = null;
                            {
                                ContentType ct = ContentType.get(entity);
                                if (ct != null) {
                                    charset = ct.getCharset();
                                }
                            }
                            if (charset == null) {
                                charset = encode;
                            }
                            return EntityUtils.toString(entity, charset);
                    }
                    return null;
                }
            }, context);
        }

        if (syncCookie && (cookieStore != null)) {
            CookieHandler handler = CookieHandler.getDefault();
            if (handler instanceof CookieManager) {
                CookieManager manager = (CookieManager) handler;
                Lock lock = cookieManagerLock.writeLock();
                try {
                    lock.lock();
                    java.net.CookieStore cs = manager.getCookieStore();
                    for (Cookie cookie : cookieStore.getCookies()) {
                        HttpCookie httpCookie = new HttpCookie(cookie.getName(), cookie.getValue());
                        httpCookie.setComment(cookie.getComment());
                        httpCookie.setCommentURL(cookie.getCommentURL());
                        httpCookie.setDomain(cookie.getDomain());
                        httpCookie.setPath(cookie.getPath());
                        httpCookie.setSecure(cookie.isSecure());
                        httpCookie.setVersion(cookie.getVersion());
                        cs.add(null, httpCookie);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        return ret;
    }
}
