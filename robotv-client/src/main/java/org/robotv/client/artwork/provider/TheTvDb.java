package org.robotv.client.artwork.provider;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.model.Event;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TheTvDb extends HttpArtworkProvider {

    private final static String TAG = "TheTvDb";
    private final static String baseUrl = "https://api.thetvdb.com";
    private final static String imageBaseUrl = "http://www.thetvdb.com/banners/";
    private final static String apiKey = "201D2A17712E85AF";

    private String language;
    static private String token;
    static private long tokenTimestamp;

    public TheTvDb(String language) {
        super(5);
        this.language = language;

    }

    private boolean login() {
        long currentTimestamp = System.currentTimeMillis();

        if(currentTimestamp - tokenTimestamp > 1000 * 60 * 60 * 23) {
            tokenTimestamp = currentTimestamp;
            token = null;
        }

        if(!TextUtils.isEmpty(token)) {
            return true;
        }

        OkHttpClient client = getClient();

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"apikey\":\"" + apiKey + "\"}");

        HttpUrl httpUrl = HttpUrl.parse(baseUrl + "/login");
        if(httpUrl == null) {
            return false;
        }

        Request request = new Request.Builder().url(httpUrl)
                .post(requestBody).build();

        Response response;
        try {
            response = client.newCall(request).execute();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if(response == null) {
            Log.e(TAG, "response is null !");
            return false;
        }

        int status = response.code();

        if(status != 200) {
            Log.e(TAG, "error login tvdb: " + status);
            return false;
        }

        JSONObject o;
        try {
            o = new JSONObject(response.body().string());
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        token = o.optString("token");
        return true;
    }

    private JSONArray request(String url, String language) {
        Request request = new Request.Builder()
                .url(baseUrl + url)
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", language)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        Response response;
        try {
            response = getClient().newCall(request).execute();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if(response == null) {
            Log.e(TAG, "response is null !");
            return null;
        }

        JSONObject o;

        try {
            String responseString = response.body().string();

            o = new JSONObject(responseString);
            JSONArray data = o.optJSONArray("data");

            if(data == null) {
                return null;
            }

            if(data.length() == 0) {
                return null;
            }

            return data;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected ArtworkHolder searchMovie(Event event) throws IOException {
        // only tv shows for tvdb
        return null;
    }

    private int search(String title) {
        JSONArray data = request("/search/series?name=" +  URLEncoder.encode(title), this.language);

        if(data == null) {
            return 0;
        }

        // get first response
        JSONObject s = data.optJSONObject(0);

        if(s == null) {
            Log.e(TAG, "first result is empty ?");
            return 0;
        }

        return s.optInt("id");
    }

    private JSONArray getFanart(int id) {
        String url = "/series/" + id + "/images/query?keyType=fanart&resolution=1920x1080&subKey=graphical";
        JSONArray data = request(url, this.language);

        if(data == null && !this.language.equals("en")) {
            data = request(url, "en");
        }

        if(data == null) {
            return new JSONArray();
        }

        return data;
    }

    private JSONArray getPosters(int id) {
        String url = "/series/" + id + "/images/query?keyType=poster";

        JSONArray data = request(url, this.language);

        if(data == null && !this.language.equals("en")) {
            data = request(url, "en");
        }

        if(data == null) {
            return new JSONArray();
        }

        return data;
    }

    public List<ArtworkHolder> searchAll(String title) {
        login();

        List<ArtworkHolder> result = new ArrayList<>();
        int id = search(title);

        if(id == 0) {
            Log.d(TAG, "no result");
            return result;
        }

        JSONArray posters = getPosters(id);
        JSONArray backgrounds = getFanart(id);

        Log.d(TAG, "posters: " + posters.length());
        Log.d(TAG, "backgrounds: " + backgrounds.length());

        int b = 0;
        for(int p = 0; p < posters.length(); p++, b++) {
            if(b >= backgrounds.length()) {
                b = 0;
            }

            JSONObject poster = posters.optJSONObject(p);
            JSONObject background = backgrounds.optJSONObject(b);

            String posterUrl = "";
            String backgroundUrl = "";

            String path = poster.optString("fileName");

            if(!TextUtils.isEmpty(path)) {
                posterUrl = imageBaseUrl + path;
            }

            if(background != null) {
                path = background.optString("fileName");

                if (!TextUtils.isEmpty(path)) {
                    backgroundUrl = imageBaseUrl + path;
                }
            }

            if(!TextUtils.isEmpty(posterUrl)) {
                ArtworkHolder holder = new ArtworkHolder(posterUrl, backgroundUrl);
                holder.setTitle(title);
                result.add(holder);
            }
        }

        return result;
    }

    @Override
    protected ArtworkHolder searchTv(Event event) throws IOException {
        login();

        int id = search(event.getTitle());

        if(id == 0) {
            return null;
        }

        // request fanart
        // https://api.thetvdb.com/series/xxxx/images/query?keyType=fanart&resolution=1920x1080&subKey=graphical

        JSONArray data = getFanart(id);

        if(data == null) {
            return null;
        }

        JSONObject s = data.optJSONObject(0);

        if(s == null) {
            return null;
        }

        String fileName = s.optString("fileName");
        String backgroundUrl = "";

        if(!TextUtils.isEmpty(fileName)) {
            backgroundUrl = imageBaseUrl + s.optString("fileName");
        }

        data = getPosters(id);

        if(data == null) {
            return null;
        }

        s = data.optJSONObject(0);

        if(s == null) {
            return null;
        }

        fileName = s.optString("fileName");
        String posterUrl = "";

        if(!TextUtils.isEmpty(fileName)) {
            posterUrl = imageBaseUrl + s.optString("fileName");
        }

        if(TextUtils.isEmpty(backgroundUrl) && TextUtils.isEmpty(posterUrl)) {
            return null;
        }

        ArtworkHolder holder = new ArtworkHolder(
                   posterUrl,
                   backgroundUrl
               );

        holder.setTitle(event.getTitle());
        return holder;
    }
}
