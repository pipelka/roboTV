package org.robotv.client.artwork;

import android.text.TextUtils;
import android.util.Log;

import org.robotv.client.model.Movie;
import org.robotv.msgexchange.Packet;
import org.robotv.client.artwork.provider.ArtworkProvider;
import org.robotv.client.artwork.provider.HttpEpgImageProvider;
import org.robotv.client.artwork.provider.RoboTvProvider;
import org.robotv.client.artwork.provider.StockImageProvider;
import org.robotv.client.artwork.provider.TheMovieDatabase;
import org.robotv.client.artwork.provider.TheTvDb;
import org.robotv.client.Connection;
import org.robotv.client.model.Event;

import java.io.IOException;

public class ArtworkFetcher {

    private static final String TAG = ArtworkFetcher.class.getName();
    public final static String TMDB_APIKEY = "958abef9265db99029a13521fddcb648";

    private final Connection mConnection;
    private final ArtworkProvider[] providers;
    private String mEpgImageTemplateUrl;

    static private TheMovieDatabase tmdb = null;

    public ArtworkFetcher(Connection connection, String language) {
        if(tmdb == null) {
            tmdb =  new TheMovieDatabase(TMDB_APIKEY, language);
        }

        mConnection = connection;

        // fetch epg images template url
        mEpgImageTemplateUrl = getEpgImageTemplateUrl();

        providers = new ArtworkProvider[5];
        providers[0] = new RoboTvProvider(connection);
        providers[1] = new HttpEpgImageProvider(mEpgImageTemplateUrl);
        providers[2] = tmdb;
        providers[3] = new TheTvDb(language);
        providers[4] = new StockImageProvider();
    }

    public boolean fetchForEvent(Event event) throws IOException {
        if(event.hasArtwork()) {
            return true;
        }

        // sanity check
        if(TextUtils.isEmpty(event.getTitle()) || providers == null) {
            return false;
        }

        if(mEpgImageTemplateUrl.isEmpty()) {
            mEpgImageTemplateUrl = getEpgImageTemplateUrl();
            providers[1] = new HttpEpgImageProvider(mEpgImageTemplateUrl);
        }

        ArtworkHolder o = null;

        // try all providers
        boolean registerOnServer = true;

        for (ArtworkProvider provider : providers) {
            if ((o = provider.search(event)) != null) {
                // skip registering the artwork if it comes from our server cache
                registerOnServer = (provider != providers[0]);
                break;
            }
        }

        // didn't get any result
        if(o == null) {
            return false;
        }

        event.setPosterUrl(o.getPosterUrl());
        event.setBackgroundUrl(o.getBackgroundUrl());

        // register artwork on server
        if(registerOnServer) {
            if(event instanceof Movie) {
                ArtworkUtils.setMovieArtwork(mConnection, (Movie)event, o);
            }
            else if(!putForEvent(event)) {
                Log.d(TAG, "failed to register artwork for '" + event.getTitle() + "' in cache");
            }
        }

        return event.hasArtwork();
    }

    public boolean putForEvent(Event event) {
        Log.d(TAG, "putForEvent: " + event.getPosterUrl());

        Packet req = mConnection.CreatePacket(Connection.ARTWORK_SET);

        req.putString(event.getTitle());
        req.putU32(event.getContentId());
        req.putString(event.getPosterUrl().equals("x") ? "" : event.getPosterUrl());
        req.putString(event.getBackgroundUrl().equals("x") ? "" : event.getBackgroundUrl());
        req.putU32(0);

        // update EPG entry
        req.putU32(event.getChannelUid());
        req.putU32(event.getEventId());

        return (mConnection.transmitMessage(req) != null);
    }

    private String getEpgImageTemplateUrl() {
        return mConnection.getConfig("EpgImageUrl");
    }
}
