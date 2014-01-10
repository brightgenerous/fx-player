package com.brightgenerous.fxplayer.url;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.brightgenerous.fxplayer.util.IVideoInfo;
import com.google.gson.Gson;

class MyServiceUtils {

    private static final String URL_HOST = "http://ec2-54-200-144-107.us-west-2.compute.amazonaws.com:3000";

    //private static final String URL_HOST = "http://localhost:3000";

    private MyServiceUtils() {
    }

    public static String getQueryUrl(String word) throws UnsupportedEncodingException {
        return URL_HOST + "/pl?key=" + URLEncoder.encode(word, "UTF-8");
    }

    public static List<IVideoInfo> fromFile(File file, File parent) {
        List<IVideoInfo> ret = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] strs = lineToStrs(line);
                if ((strs == null) || (strs[0] == null)) {
                    continue;
                }
                String path = null;
                {
                    String tmp = strs[0];
                    {
                        if (parent != null) {
                            File f = new File(parent, tmp);
                            if (f.exists()) {
                                try {
                                    path = f.toURI().toURL().toString();
                                } catch (MalformedURLException e) {
                                }
                            }
                        }
                    }
                    if (path == null) {
                        File f = new File(tmp);
                        if (f.exists()) {
                            try {
                                path = f.toURI().toURL().toString();
                            } catch (MalformedURLException e) {
                            }
                        }
                    }
                    if (path == null) {
                        try {
                            path = new URL(tmp).toString();
                        } catch (MalformedURLException e) {
                        }
                    }
                }
                if (path != null) {
                    String desc = (strs[1] == null) ? strs[0] : strs[1];
                    ret.add(new VideoInfo(path, desc));
                }
            }
        } catch (IOException e) {
        }
        return ret;
    }

    private static String[] lineToStrs(String line) {
        String[] ret = new String[2];
        if ((line != null)) {
            String[] strs = line.trim().split("\t+");
            ret[0] = strs[0].trim();
            for (int i = 1; i < strs.length; i++) {
                String str = strs[i].trim();
                if (!str.isEmpty()) {
                    if (!str.startsWith("#")) {
                        ret[1] = str;
                    }
                    break;
                }
            }
        }
        return ret;
    }

    public static List<IVideoInfo> fromServer(String text, String serverPath, String dirPath) {
        List<IVideoInfo> ret = new ArrayList<>();
        JsonResponse response = new Gson().fromJson(text, JsonResponse.class);
        for (JsonResponseDataFile file : response.getData().getFiles()) {
            ret.add(new VideoInfo(file.getUrl(), file.getDescription()));
        }
        return ret;
    }

    private static class JsonResponse {

        private JsonResponseData data;

        public JsonResponseData getData() {
            return data;
        }

        public void setData(JsonResponseData data) {
            this.data = data;
        }
    }

    private static class JsonResponseData {

        private List<JsonResponseDataFile> files;

        public List<JsonResponseDataFile> getFiles() {
            return files;
        }

        public void setFiles(List<JsonResponseDataFile> files) {
            this.files = files;
        }
    }

    private static class JsonResponseDataFile {

        private String url;

        private String description;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
