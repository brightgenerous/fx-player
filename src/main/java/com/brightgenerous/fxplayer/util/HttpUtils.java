package com.brightgenerous.fxplayer.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class HttpUtils {

    private HttpUtils() {
    }

    public static String execGet(String url, Charset encode) throws IOException {
        return execGet(url, null, encode);
    }

    public static String execGet(String url, String userAgent, Charset encode) throws IOException {
        return exec(false, url, userAgent, null, null, encode, false);
    }

    public static String execPost(String url, Charset encode) throws IOException {
        return execPost(url, null, encode);
    }

    public static String execPost(String url, String userAgent, Charset encode) throws IOException {
        return execPost(url, userAgent, null, null, encode);
    }

    public static String execPost(String url, String userAgent, String body, String contentType,
            Charset encode) throws IOException {
        return exec(true, url, userAgent, body, contentType, encode, false);
    }

    private static String exec(boolean post, String url, String userAgent, String body,
            String contentType, final Charset encode, boolean selfSigned) throws IOException {

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

        return ret;
    }
}
