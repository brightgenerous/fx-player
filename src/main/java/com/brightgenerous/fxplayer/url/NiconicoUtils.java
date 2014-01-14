package com.brightgenerous.fxplayer.url;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.brightgenerous.fxplayer.util.IVideoInfo;

class NiconicoUtils {

    private static final String URL_HOST = "http://www.nicovideo.jp";

    private static final Pattern pagePattern = Pattern.compile("(.*(?:\\?|&)page=)(\\d*)(.*)");

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

    public static String getQueryUrl(String word, Integer page) throws UnsupportedEncodingException {
        if (word == null) {
            return null;
        }
        String qp = (page == null) ? "" : ("&page=" + page);
        return URL_HOST + "/search/" + URLEncoder.encode(word, "UTF-8") + "?sort=v&order=d" + qp;
    }

    public static String getQueryPageUrl(String url, int inc) {
        if (url == null) {
            return null;
        }
        if (!url.contains("nicovideo.jp/search/")) {
            return null;
        }
        String ret = null;
        Matcher matcher = pagePattern.matcher(url);
        if (matcher.find()) {
            String _u1 = matcher.group(1);
            String _p = matcher.group(2);
            String _u2 = matcher.group(3);
            int page = 1;
            try {
                page = Integer.parseInt(_p) + inc;
            } catch (NumberFormatException e) {
            }
            if (0 < page) {
                ret = _u1 + page + _u2;
            }
        } else {
            int page = 1 + inc;
            if (0 < page) {
                if (url.contains("?")) {
                    ret = url + "&page=" + page;
                } else {
                    ret = url + "?page=" + page;
                }
            }
        }
        return ret;
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

    public static boolean isPlaylistUrl(String url) {
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
            return path.startsWith("/search") || path.startsWith("/mylist");
        } catch (MalformedURLException e) {
        }

        return false;
    }

    public static List<IVideoInfo> parsePlaylist(String url, String html) {
        if (html == null) {
            return null;
        }

        boolean isSearch = false;
        try {
            URL _url = new URL(url);
            String host = _url.getHost();
            if (host == null) {
                return null;
            }
            if (!host.contains("nicovideo.jp")) {
                return null;
            }
            String path = _url.getPath();
            if (path == null) {
                return null;
            }
            isSearch = path.startsWith("/search");
        } catch (MalformedURLException e) {
        }

        if (isSearch) {
            return parsePlaylistSearch(html);
        }
        return parsePlaylistMylist(html);
    }

    private static final Pattern anchorSearch = Pattern
            .compile("<p[^>]*\\sclass\\s*=\\s*\"(?:[^\"]*//s)?itemTitle(?://s[^\"]*)?\">\\s*"
                    + "<a" + "(?:[^>]*\\stitle\\s*=\\s*\"([^\"]*)\")?"
                    // [x] keep only first argument
                    + "[^>]*\\shref\\s*=\\s*\"(/watch[^&\"]*)[^\"]*\""
                    + "(?:[^>]*\\stitle\\s*=\\s*\"([^\"]*)\")?" + "[^>]*>" + "\\s*([^<]*)\\s*</a>");

    public static List<IVideoInfo> parsePlaylistSearch(String html) {
        List<IVideoInfo> ret = new ArrayList<>();

        Map<String, VideoInfo> infos = new HashMap<>();
        Matcher matcher = anchorSearch.matcher(html);
        while (matcher.find()) {
            String href = matcher.group(2);
            if (href.contains("ref=search_key_trendvideo")) {
                continue;
            }
            String title;
            {
                String title1 = matcher.group(1);
                String title2 = matcher.group(3);
                if ((title1 == null) || (title2 == null)) {
                    if (title1 != null) {
                        title = title1;
                    } else if (title2 != null) {
                        title = title2;
                    } else {
                        title = null;
                    }
                } else if (title1.length() < title2.length()) {
                    title = title2;
                } else {
                    title = title1;
                }
                {
                    String title3 = matcher.group(4);
                    if (title3 != null) {
                        if (title == null) {
                            title = title3;
                        } else if (title.length() < title3.length()) {
                            title = title3;
                        }
                    }
                }
                if (title != null) {
                    title = title.replace("&#39;", "'").replace("&quot;", "\"")
                            .replace("&amp;", "&").trim();
                }
            }
            String _url = URL_HOST + href;
            VideoInfo info = infos.get(_url);
            if (info == null) {
                info = new VideoInfo(_url, title);
                infos.put(_url, info);
                ret.add(info);
            } else {
                if (info.getTitle() == null) {
                    info.setTitle(title);
                }
            }
        }

        return ret;
    }

    private static final Pattern anchorMylist = Pattern
            .compile("\"item_data\"\\s*:\\s*(\\{[^}]*\\})");

    private static final Pattern anchorMylistT = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]*)\"");

    private static final Pattern anchorMylistW = Pattern
            .compile("\"watch_id\"\\s*:\\s*\"([^\"]*)\"");

    public static List<IVideoInfo> parsePlaylistMylist(String html) {
        List<IVideoInfo> ret = new ArrayList<>();

        Map<String, VideoInfo> infos = new HashMap<>();
        Matcher matcher = anchorMylist.matcher(html);
        while (matcher.find()) {
            String data = matcher.group(1);
            String title = null;
            {
                Matcher m = anchorMylistT.matcher(data);
                if (m.find()) {
                    title = m.group(1);
                }
            }
            if (title == null) {
                continue;
            }
            String watchId = null;
            {
                Matcher m = anchorMylistW.matcher(data);
                if (m.find()) {
                    watchId = m.group(1);
                }
            }
            if (watchId == null) {
                continue;
            }
            {
                title = unescape(title);
            }
            String _url = URL_HOST + "/watch/" + watchId;
            VideoInfo info = infos.get(_url);
            if (info == null) {
                info = new VideoInfo(_url, title);
                infos.put(_url, info);
                ret.add(info);
            } else {
                if (info.getTitle() == null) {
                    info.setTitle(title);
                }
            }
        }

        return ret;
    }

    private static String unescape(String str) {
        StringBuilder sb = new StringBuilder();
        String[] codeStrs = str.split("\\\\u");
        for (String codeStr : codeStrs) {
            if (codeStr.isEmpty()) {
                continue;
            }
            try {
                if (codeStr.length() <= 4) {
                    sb.append(Character.valueOf((char) Integer.parseInt(codeStr, 16)));
                } else {
                    sb.append(Character.valueOf((char) Integer.parseInt(codeStr.substring(0, 4), 16)));
                    sb.append(codeStr.substring(4));
                }
            } catch (NumberFormatException e) {
                sb.append(codeStr);
            }
        }
        return sb.toString();
    }

    private static final Pattern patternTitle = Pattern
            .compile("<h1[^>]*\\sitemprop\\s*=\\s*\"name\"[^>]*>([^<]*)</h1>");

    public static String extractTitle(String html) {
        if (html == null) {
            return null;
        }

        Matcher matcher = patternTitle.matcher(html);
        if (matcher.find()) {
            String title = matcher.group(1);
            if (title != null) {
                title = title.trim();
            }
            return title;
        }
        return null;
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