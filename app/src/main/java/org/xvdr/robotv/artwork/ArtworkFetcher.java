package org.xvdr.robotv.artwork;

import android.text.TextUtils;
import android.util.Log;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.artwork.provider.ArtworkProvider;
import org.xvdr.robotv.artwork.provider.HttpEpgImageProvider;
import org.xvdr.robotv.artwork.provider.RoboTvProvider;
import org.xvdr.robotv.artwork.provider.StockImageProvider;
import org.xvdr.robotv.artwork.provider.TheMovieDatabase;
import org.xvdr.robotv.artwork.provider.TheTvDb;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.model.Event;

import java.io.IOException;

public class ArtworkFetcher {

    private static final String TAG = "ArtworkFetcher";
    public final static String TMDB_APIKEY = "958abef9265db99029a13521fddcb648";

    private Connection mConnection;
    private ArtworkProvider mServerCache;
    private ArtworkProvider[] mProviders;
    private String mEpgImageTemplateUrl = "";

    public ArtworkFetcher(Connection connection, String language) {
        mConnection = connection;
        mServerCache = new RoboTvProvider(connection);

        // fetch epg images template url
        mEpgImageTemplateUrl = getEpgImageTemplateUrl();

        mProviders = new ArtworkProvider[4];
        mProviders[0] = new HttpEpgImageProvider(mEpgImageTemplateUrl);
        mProviders[1] = new TheMovieDatabase(TMDB_APIKEY, language);
        mProviders[2] = new TheTvDb(language);
        mProviders[3] = new StockImageProvider();
    }

    public ArtworkHolder fetchForEvent(Event event) throws IOException {
        // sanity check
        if(TextUtils.isEmpty(event.getTitle()) || mProviders == null) {
            return null;
        }

        if(mEpgImageTemplateUrl.isEmpty()) {
            mEpgImageTemplateUrl = getEpgImageTemplateUrl();
            mProviders[0] = new HttpEpgImageProvider(mEpgImageTemplateUrl);
        }

        // check server cache first
        ArtworkHolder o;

        if((o = mServerCache.search(event)) != null) {
            return o;
        }

        // try all providers
        for(ArtworkProvider provider : mProviders) {
            if((o = provider.search(event)) != null) {
                break;
            }
        }

        // didn't get any result
        if(o == null) {
            return null;
        }

        // register artwork on server
        Packet req = mConnection.CreatePacket(Connection.XVDR_ARTWORK_SET);
        req.putString(event.getTitle());
        req.putU32(event.getContentId());
        req.putString(o.getPosterUrl());
        req.putString(o.getBackgroundUrl());
        req.putU32(0);

        // update EPG entry
        req.putU32(event.getChannelUid());
        req.putU32(event.getEventId());

        if(mConnection.transmitMessage(req) == null) {
            Log.d(TAG, "failed to register artwork for '" + event.getTitle() + "' in cache");
        }

        return o;
    }

    private String getEpgImageTemplateUrl() {
        return mConnection.getConfig("EpgImageUrl");
    }
}
