package org.robotv.client.artwork;

import android.text.TextUtils;
import android.util.Log;

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

    private static final String TAG = "ArtworkFetcher";
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

        // register artwork on server
        if(registerOnServer) {
            Packet req = mConnection.CreatePacket(Connection.XVDR_ARTWORK_SET);
            req.putString(event.getTitle());
            req.putU32(event.getContentId());
            req.putString(o.getPosterUrl().equals("x") ? "" : o.getPosterUrl());
            req.putString(o.getBackgroundUrl().equals("x") ? "" : o.getBackgroundUrl());
            req.putU32(0);

            // update EPG entry
            req.putU32(event.getChannelUid());
            req.putU32(event.getEventId());

            if (mConnection.transmitMessage(req) == null) {
                Log.d(TAG, "failed to register artwork for '" + event.getTitle() + "' in cache");
            }
        }

        event.setPosterUrl(o.getPosterUrl());
        event.setBackgroundUrl(o.getBackgroundUrl());

        return event.hasArtwork();
    }

    private String getEpgImageTemplateUrl() {
        return mConnection.getConfig("EpgImageUrl");
    }
}
