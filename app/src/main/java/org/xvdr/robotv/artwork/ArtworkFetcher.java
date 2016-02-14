package org.xvdr.robotv.artwork;

import android.util.Log;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.artwork.provider.ArtworkProvider;
import org.xvdr.robotv.artwork.provider.RoboTvProvider;
import org.xvdr.robotv.artwork.provider.StockImageProvider;
import org.xvdr.robotv.artwork.provider.TheMovieDatabase;
import org.xvdr.robotv.tv.ServerConnection;

import java.io.IOException;

public class ArtworkFetcher {

    static final String TAG = "ArtworkFetcher";
    private final static String TMDB_APIKEY = "958abef9265db99029a13521fddcb648";

    ServerConnection mConnection;
    ArtworkProvider mServerCache;
    ArtworkProvider[] mProviders;

    public ArtworkFetcher(ServerConnection connection, String language) {
        mConnection = connection;
        mServerCache = new RoboTvProvider(connection);

        mProviders = new ArtworkProvider[2];
        mProviders[0] = new TheMovieDatabase(TMDB_APIKEY, language);
        mProviders[1] = new StockImageProvider();


    }

    public ArtworkHolder fetchForEvent(Event event) throws IOException {
        // sanity check
        if(event.getTitle().isEmpty() || mProviders == null) {
            return null;
        }

        // check server cache first
        ArtworkHolder o;
        if((o = mServerCache.search(event)) != null) {
            return o;
        }

        // try all providers
        for(ArtworkProvider provider: mProviders) {
            if((o = provider.search(event)) != null) {
                break;
            };
        }

        // didn't get any result
        if(o == null) {
            return null;
        }

        // register artwork on server
        Packet req = mConnection.CreatePacket(ServerConnection.XVDR_ARTWORK_SET);
        req.putString(event.getTitle());
        req.putU32(event.getContentId());
        req.putString(o.getPosterUrl());
        req.putString(o.getBackgroundUrl());
        req.putU32(0);

        if(mConnection.transmitMessage(req) == null) {
            Log.d(TAG, "failed to register artwork for '" + event.getTitle() + "' in cache");
        }
        else if (!o.getBackgroundUrl().isEmpty()) {
            Log.d(TAG, "putting artwork for '" + event.getTitle() + "' into cache (0x" + Integer.toHexString(event.getContentId()) + ")");
        }

        return o;
    }
}
