package org.xvdr.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.SparseArray;

import org.xvdr.robotv.client.Channels;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public abstract class SyncChannelIconsTask extends AsyncTask<Void, Void, Void> {

    private Context mContext;
    private String mInputId;
    private Connection mConnection;
    private  ContentResolver mResolver;

    final private byte[] mBuffer = new byte[4096];

    public SyncChannelIconsTask(Connection connection, Context context, String inputId) {
        mConnection = connection;
        mInputId = inputId;
        mResolver = context.getContentResolver();
        mContext = context;
    }

    private void fetchChannelLogo(Uri channelUri, String address) {
        URL sourceUrl;
        OutputStream os;
        InputStream in;
        URLConnection urlConnection;
        Uri channelLogoUri = TvContract.buildChannelLogoUri(channelUri);

        try {
            os = mResolver.openOutputStream(channelLogoUri);
            sourceUrl = new URL(address);
            urlConnection = sourceUrl.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream());
        }
        catch(Exception e) {
            return;
        }

        if(os == null) {
            return;
        }

        int bytes_read;

        try {
            while((bytes_read = in.read(mBuffer)) > 0) {
                os.write(mBuffer, 0, bytes_read);
            }

            in.close();
            os.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        final SparseArray<Long> existingChannels = new SparseArray<>();

        ChannelSyncAdapter.getExistingChannels(mResolver, mInputId, existingChannels);

        Channels list = new Channels();
        String language = SetupUtils.getLanguageISO3(mContext);

        list.load(mConnection, language, new Channels.Callback() {
            @Override
            public boolean onChannel(final Channels.Entry entry) {
                Long channelId = existingChannels.get(entry.uid);

                // exit if task is cancelled
                if(isCancelled()) {
                    return false;
                }

                if(channelId == null) {
                    return true;
                }

                final Uri uri = TvContract.buildChannelUri(channelId);
                fetchChannelLogo(uri, entry.iconURL);

                return true;
            }
        });

        return null;
    }
}
