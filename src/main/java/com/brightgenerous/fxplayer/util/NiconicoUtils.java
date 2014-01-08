package com.brightgenerous.fxplayer.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NiconicoUtils {

    private static volatile String mail;

    private static volatile String pass;

    private static final HttpUtils http = HttpUtilsBuilder.createDefault()
            .contentType("application/x-www-form-urlencoded").syncCookie(true).build();

    private NiconicoUtils() {
    }

    public static String getMail() {
        return mail;
    }

    public static void setMail(String mail) {
        NiconicoUtils.mail = mail;
    }

    public static String getPass() {
        return pass;
    }

    public static void setPass(String pass) {
        NiconicoUtils.pass = pass;
    }

    public static boolean isVideoUrl(String url) {
        if (url == null) {
            return false;
        }

        try {
            URL _url = new URL(url);
            String host = _url.getHost();
            if (host == null) {
                return false;
            }
            if (!host.contains("nicovideo.jp")) {
                return false;
            }
            String path = _url.getPath();
            if (path == null) {
                return false;
            }
            return path.startsWith("/watch");
        } catch (MalformedURLException e) {
        }

        return false;
    }

    private static final Pattern urlPattern = Pattern.compile("url=([^&]*)");

    private static final Pattern idPattern = Pattern.compile("/([^/?&]*)(?:\\?.*|&.*|)$");

    public static String extractUrl(String url) throws IOException {
        if (url == null) {
            return null;
        }

        String id = null;
        {
            Matcher matcher = idPattern.matcher(url);
            if (matcher.find()) {
                id = URLDecoder.decode(matcher.group(1), "UTF-8");
            }
        }

        String ret = null;
        String _mail = mail;
        String _pass = pass;
        for (int i = 0; i < 2; i++) {
            if ((_mail == null) || (_pass == null)) {
                break;
            }
            if (findFailed(_mail, _pass)) {
                break;
            }
            if (0 < i) {
                String html = http.execPost(
                        "https://secure.nicovideo.jp/secure/login?site=niconico",
                        "next_url=&mail_tel=" + _mail + "&password=" + _pass);
                if (html.matches("<form\\s*[^>]*\\s*action\\s*=\\s*\"https://secure.nicovideo.jp/secure/login")) {
                    registFailed(_mail, _pass);
                    break;
                }
                registSuccess(_mail, _pass);
            }
            String info = http.execGet("http://flapi.nicovideo.jp/api/getflv/" + id);
            if (info != null) {
                Matcher matcher = urlPattern.matcher(info);
                if (matcher.find()) {
                    ret = URLDecoder.decode(matcher.group(1), "UTF-8");
                    break;
                }
            }
        }

        if (ret != null) {
            // for set session to cookie.
            http.execGet("http://www.nicovideo.jp/watch/" + id);
        }

        return ret;
    }

    private static final ReadWriteLock accountsLock = new ReentrantReadWriteLock();

    private static final List<Account> accounts = new ArrayList<>();

    private static boolean findFailed(String mail, String pass) {
        Lock lock = accountsLock.readLock();
        try {
            lock.lock();
            return accounts.contains(new Account(mail, pass));
        } finally {
            lock.unlock();
        }
    }

    private static void registSuccess(String mail, String pass) {
        if (!findFailed(mail, pass)) {
            return;
        }
        Account account = new Account(mail, pass);
        Lock lock = accountsLock.writeLock();
        try {
            lock.lock();
            if (accounts.contains(account)) {
                accounts.remove(account);
            }
        } finally {
            lock.unlock();
        }
    }

    private static void registFailed(String mail, String pass) {
        if (findFailed(mail, pass)) {
            return;
        }
        Account account = new Account(mail, pass);
        Lock lock = accountsLock.writeLock();
        try {
            lock.lock();
            if (!accounts.contains(account)) {
                accounts.add(account);
                if (100 < accounts.size()) {
                    accounts.removeAll(accounts.subList(0, 50));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private static class Account {

        private String mail;

        private String pass;

        public Account(String mail, String pass) {
            this.mail = mail;
            this.pass = pass;
        }

        @Override
        public int hashCode() {
            int ret = 17;
            ret += (mail == null) ? -1 : mail.hashCode();
            ret *= 31;
            ret += (pass == null) ? -1 : pass.hashCode();
            return ret;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Account)) {
                return false;
            }
            Account arg = (Account) obj;
            if (mail != arg.mail) {
                if ((mail == null) || (arg.mail == null)) {
                    return false;
                }
                if (!mail.equals(arg.mail)) {
                    return false;
                }
            }
            if (pass != arg.pass) {
                if ((pass == null) || (arg.pass == null)) {
                    return false;
                }
                if (!pass.equals(arg.pass)) {
                    return false;
                }
            }
            return true;
        }
    }
}