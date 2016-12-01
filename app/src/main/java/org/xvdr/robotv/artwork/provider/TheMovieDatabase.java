package org.xvdr.robotv.artwork.provider;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.client.model.Event;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class TheMovieDatabase extends HttpArtworkProvider {

    private final static String TAG = "TheMovieDatabase";
    private final static String IMAGE_BASE_PATH = "http://image.tmdb.org/t/p/w1280";

    private String mApiKey;
    private String mLanguage;

    public TheMovieDatabase(String apiKey, String language) {
        super(1);
        mApiKey = apiKey;
        mLanguage = language;

        setDelayAfterRequest(200);
    }

    private String simplifyString(String title) {
        String result = title.toLowerCase();
        result = result.replaceAll(",", "").replaceAll("-", "").replaceAll("  ", " ");

        return result;
    }

    private JSONArray filterTitleMatch(String title, String titleTag, JSONArray array) {
        if(array == null) {
            return null;
        }

        JSONArray results = new JSONArray();
        String a = simplifyString(title);

        for(int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);

            if(item == null) {
                continue;
            }

            String b = simplifyString(item.optString(titleTag));

            if(a.equals(b)) {
                results.put(item);
            }
        }

        if(results.length() > 0) {
            return results;
        }

        return array;
    }

    private List<ArtworkHolder> getArtworkList(JSONArray array) {
        List<ArtworkHolder> result = new ArrayList<>();

        for(int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);

            if(item == null) {
                continue;
            }

            String poster = item.optString("poster_path");
            String background = item.optString("backdrop_path");
            String title = item.optString("title");

            if(poster == null || poster.equals("null")) {
                continue;
            }

            if(background == null || background.equals("null")) {
                background = "";
            }

            if(!background.isEmpty()) {
                background = IMAGE_BASE_PATH + background;
            }

            ArtworkHolder holder = new ArtworkHolder(
                IMAGE_BASE_PATH + poster,
                background
            );

            holder.setTitle(title);

            Log.d(TAG, IMAGE_BASE_PATH + poster);
            result.add(holder);
        }

        return result;
    }

    public List<ArtworkHolder> searchAll(String title) {
        List<ArtworkHolder> result = new ArrayList<>();
        JSONArray array;

        // movie search
        try {
            array = search("movie", title, 0, null);
        }
        catch(Exception e) {
            e.printStackTrace();
            return result;
        }

        result.addAll(getArtworkList(array));

        // tv search
        try {
            array = search("tv", title, 0, null);
        }
        catch(Exception e) {
            e.printStackTrace();
            return result;
        }

        result.addAll(getArtworkList(array));

        return result;
    }

    protected JSONArray search(String section, String title, int year, String dateProperty) throws IOException, JSONException {
        String request;

        request = "http://api.themoviedb.org/3/search/" + section + "?api_key=" + mApiKey + "&query=" + URLEncoder.encode(title);

        if(!mLanguage.isEmpty()) {
            request += "&language=" + mLanguage;
        }

        JSONObject o = new JSONObject(getResponseFromServer(request));
        JSONArray results = filterTitleMatch(
                                title,
                                section.equals("tv") ? "name" : "title",
                                o.optJSONArray("results"));

        // filter entries (search for year)
        if(year > 0 && !dateProperty.isEmpty()) {
            JSONArray a = new JSONArray();

            for(int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);

                if(item == null) {
                    continue;
                }

                String date = item.optString(dateProperty);

                if(date == null || date.length() < 4) {
                    continue;
                }

                int entryYear = Integer.parseInt(date.substring(0, 4));

                // release year differs often
                if(year == entryYear || year == entryYear - 1) {
                    a.put(item);
                }
            }

            if(a.length() > 0) {
                return a;
            }
        }

        return results;
    }

    private String getUrl(JSONArray results, String property) {
        if(results == null) {
            return "";
        }

        String path = "";

        for(int i = 0; i < results.length(); i++) {
            JSONObject item = results.optJSONObject(i);

            if(item == null) {
                continue;
            }

            path = item.optString(property);

            if(path != null && !path.equals("null")) {
                break;
            }
        }

        if(path == null || path.isEmpty() || path.equals("null")) {
            return "";
        }

        return IMAGE_BASE_PATH + path;
    }

    private String getPosterUrl(JSONArray o) {
        return getUrl(o, "poster_path");
    }

    private String getBackgroundUrl(JSONArray o) {
        return getUrl(o, "backdrop_path");
    }

    @Override
    protected ArtworkHolder searchMovie(Event event) throws IOException {
        JSONArray results;

        try {
            results = search("movie", event.getTitle(), event.getYear(), "release_date");
        }
        catch(JSONException e) {
            return null;
        }

        return new ArtworkHolder(
                   getPosterUrl(results),
                   getBackgroundUrl(results)
               );
    }

    @Override
    protected ArtworkHolder searchTv(Event event) throws IOException {
        JSONArray results;

        try {
            results = search("tv", event.getTitle(), event.getYear(), "first_air_date");
        }
        catch(JSONException e) {
            return null;
        }

        String posterUrl = getPosterUrl(results);
        String backgroundUrl = getBackgroundUrl(results);

        if(TextUtils.isEmpty(posterUrl) && TextUtils.isEmpty(backgroundUrl)) {
            return null;
        }

        return new ArtworkHolder(
                   posterUrl,
                   backgroundUrl
               );
    }
}
