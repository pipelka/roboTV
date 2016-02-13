package org.xvdr.robotv.artwork;

import android.util.Log;

import org.json.JSONArray;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.tmdb.TheMovieDatabase;
import org.xvdr.robotv.tv.ServerConnection;

import java.util.Calendar;

public class ArtworkFetcher {

    static final String TAG = "ArtworkFetcher";

    private static String[] genreFilm = {
            "abenteuerfilm",
            "actiondrama",
            "actionfilm",
            "animationsfilm",
            "agentenfilm",
            "drama",
            "fantasyfilm",
            "fantasy-film",
            "gangsterfilm",
            "heimatfilm",
            "horrorfilm",
            "historienfilm",
            "jugendfilm",
            "kömödie",
            "kriegsfilm",
            "kriminalfilm",
            "monumentalfilm",
            "mysteryfilm",
            "spielfilm",
            "thriller",
            "zeichentrickfilm"
    };

    private static String[] genreSoap = {
            "folge ",
            "serie",
            "sitcom"
    };

    ServerConnection mConnection;
    TheMovieDatabase mMovieDb;

    public ArtworkFetcher(ServerConnection connection, TheMovieDatabase movieDb) {
        mConnection = connection;
        mMovieDb = movieDb;
    }

    public ArtworkHolder fetch(int contentId, String title, String subtitle, String plot) {
        // skip empty entries
        if(title.isEmpty()) {
            return null;
        }

        // guess year
        int year = 0;
        if(plot != null) {
            year = guessYearFromDescription(plot);
        }

        if(year == 0) {
            year = guessYearFromDescription(subtitle);
        }

        // guess genre
        contentId = guessGenreFromSubtitle(contentId, subtitle);

        // request artwork from server cache
        Packet areq = mConnection.CreatePacket(ServerConnection.XVDR_ARTWORK_GET);
        areq.putString(title);
        areq.putU32(contentId);

        Packet aresp = mConnection.transmitMessage(areq);
        String posterUrl = aresp.getString();
        String backgroundUrl = aresp.getString();

        ArtworkHolder artwork = new ArtworkHolder(title, contentId, plot);
        artwork.setContentId(contentId);

        if (!posterUrl.equals("x") && !backgroundUrl.equals("x")) {
            artwork.setPosterUrl(posterUrl);
            artwork.setBackgroundUrl(backgroundUrl);

            if(artwork.hasBackground()) {
                Log.d(TAG, "found artwork for '" + artwork.title + "' (0x" + Integer.toHexString(artwork.getContentId()) + ") in cache");
            }

            return artwork;
        }

        JSONArray o = null;

        try {
            // search tv series
            if (artwork.getContentId() == 0x15 || artwork.getGenre() == 0x50) {
                o = mMovieDb.searchTv(artwork.title);
            }
            // skip adult movies
            else if (artwork.getContentId() == 0x18) {
                o = null;
            }
            // search movies
            else if (artwork.getGenre() == 0x10 || (artwork.getGenre() == 0x70 && year > 0)) {
                o = mMovieDb.searchMovie(artwork.title, year);
            }
        }
        catch(Exception e) {
            Log.d(TAG, "error fetching artwork for '" + artwork.title + "' (0x" + Integer.toHexString(artwork.getContentId()) + ") from tmdb");
            return null;
        }

        if (o == null) {
            if(setStockBackground(artwork)) {
                return artwork;
            }

            artwork.setPosterUrl("");
            artwork.setBackgroundUrl("");
        }
        else {
            artwork.setPosterUrl(mMovieDb.getPosterUrl(o));
            artwork.setBackgroundUrl(mMovieDb.getBackgroundUrl(o));
        }

        // register artwork on server
        Packet req = mConnection.CreatePacket(ServerConnection.XVDR_ARTWORK_SET);
        req.putString(artwork.title);
        req.putU32(artwork.getContentId());
        req.putString(artwork.getPosterUrl());
        req.putString(artwork.getBackgroundUrl());
        req.putU32(0);

        if(mConnection.transmitMessage(req) == null) {
            Log.d(TAG, "failed to register artwork for '" + artwork.title + "' in cache");
            return null;
        }

        if (!artwork.getBackgroundUrl().isEmpty()) {
            Log.d(TAG, "putting artwork for '" + artwork.title + "' into cache (0x" + Integer.toHexString(artwork.getContentId()) + ")");
        }
        else {
            Log.d(TAG, "artwork for '" + artwork.title + "' (0x" + Integer.toHexString(artwork.getContentId()) + ") is empty");
        }

        return artwork;
    }

    static public int guessYearFromDescription(String description) {
        String[] words = description.split("[\\.,;)| ]");
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        for(String word: words) {
            word = word.replaceAll("[^\\d]", "");
            int year = 0;

            try {
                year = Integer.parseInt(word);
            }
            catch(Exception e) {
            }

            // found a match
            if(year > 1940 && year <= currentYear) {
                return year;
            }
        }

        return 0;
    }

    static int guessGenreFromSubtitle(int contentId, String subtitle) {
        for(String word: genreSoap) {
            if(subtitle.toLowerCase().contains(word.toLowerCase())) {
                return 0x15;
            }
        }

        if((contentId & 0xF0) == 0x10) {
            return contentId;
        }

        for(String word: genreFilm) {
            if(subtitle.toLowerCase().contains(word.toLowerCase())) {
                return 0x10;
            }
        }

        return contentId;
    }

    boolean setStockBackground(ArtworkHolder artwork) {
        switch(artwork.getContentId()) {
            // Football / Soccer
            case 0x43:
                artwork.setBackgroundUrl("https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-football.jpg");
                return true;
            // Tennis
            case 0x44:
                artwork.setBackgroundUrl("https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-tennis.jpg");
                return true;
            // Athletics
            case 0x46:
                artwork.setBackgroundUrl("https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-athletics.jpg");
                return true;
            // Wintersports
            case 0x49:
                artwork.setBackgroundUrl("https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-winter.jpg");
                return true;
            // Equestrian
            case 0x4A:
                artwork.setBackgroundUrl("https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-equestrian.jpg");
                return true;
        }


        return false;
    };
}
