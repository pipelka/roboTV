package org.xvdr.robotv.tmdb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class TheMovieDatabase {

    private String mApiKey;
    private String mLanguage;

    public TheMovieDatabase(String apiKey, String language) {
        mApiKey = apiKey;
        mLanguage = language;
    }

    private static String getJsonFromServer(String url) throws IOException {
        BufferedReader inputStream;

        URL jsonUrl = new URL(url);
        URLConnection dc = jsonUrl.openConnection();

        dc.setConnectTimeout(5000);
        dc.setReadTimeout(5000);

        inputStream = new BufferedReader(new InputStreamReader(dc.getInputStream()));

        String result = inputStream.readLine();

        try {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    protected JSONArray search(String section, String title) {
        return search(section, title, 0);
    }

    protected JSONArray search(String section, String title, int year) {
        String request;

        request = "http://api.themoviedb.org/3/search/" + section + "?api_key=" + mApiKey + "&query=" + URLEncoder.encode(title);

        if (!mLanguage.isEmpty()) {
            request += "&language=" + mLanguage;
        }

        JSONObject o;

        try {
            o = new JSONObject(getJsonFromServer(request));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return o.optJSONArray("results");
    }

    public JSONArray searchMovie(String title) {
        return searchMovie(title, 0);
    }

    public JSONArray searchMovie(String title, int year) {
        return search("movie", title, year);
    }

    public JSONArray searchTv(String title) {
        return search("tv", title);
    }

    protected String getUrl(JSONArray results, String property) {
        if(results == null) {
            return "";
        }

        String path = "";
        for(int i = 0; i < results.length(); i++) {
            JSONObject item = results.optJSONObject(i);

            if (item == null) {
                continue;
            }

            path = item.optString(property);
            if (path.isEmpty() || path.equals("null")) {
                continue;
            }
        }

        if(path.isEmpty() || path.equals("null")) {
            return "";
        }

        return "http://image.tmdb.org/t/p/w500" + path;
    }

    public String getPosterUrl(JSONArray o) {
        return getUrl(o, "poster_path");
    }

    public String getBackgroundUrl(JSONArray o) {
        return getUrl(o, "backdrop_path");
    }

}
