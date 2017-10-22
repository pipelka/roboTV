package org.xvdr.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.xvdr.robotv.client.Channels;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.model.Channel;
import org.xvdr.robotv.setup.SetupUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

abstract class SyncChannelIconsTask extends AsyncTask<Void, Void, Void> {

    private Context mContext;
    private String mInputId;
    private Connection mConnection;
    private ContentResolver mResolver;
    private OkHttpClient client;

    final private byte[] mBuffer = new byte[4096];
    final private static String TAG = SyncChannelIconsTask.class.getName();

    SyncChannelIconsTask(Connection connection, Context context, String inputId) {
        mConnection = connection;
        mInputId = inputId;
        mResolver = context.getContentResolver();
        mContext = context;

        client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    private void fetchChannelLogo(Uri channelUri, String address) throws IOException {
        if(TextUtils.isEmpty(address)) {
            return;
        }

        SyncUtils.ChannelHolder holder = new SyncUtils.ChannelHolder();

        if(!SyncUtils.getChannelInfo(mResolver, channelUri, holder)) {
            Log.e(TAG, String.format("unknown channel uri: '%s'", channelUri.toString()));
            return;
        }

        Uri channelLogoUri = TvContract.buildChannelLogoUri(channelUri);

        Log.d(TAG, String.format("fetching logo for channel %d: %s", holder.displayNumber, address));

        HttpUrl httpUrl = HttpUrl.parse(address);
        if(httpUrl == null) {
            Log.e(TAG, String.format("unable parse uri: %s", address));
            return;
        }

        Request request = new Request.Builder().url(httpUrl).build();
        Response response = client.newCall(request).execute();

        InputStream in = new BufferedInputStream(response.body().byteStream());
        OutputStream os = mResolver.openOutputStream(channelLogoUri);

        if(os == null) {
            Log.e(TAG, String.format("error creating logo: %s", channelLogoUri.toString()));
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
        final SortedMap<Integer, Uri> existingChannels = new TreeMap<>();

        ChannelSyncAdapter.getExistingChannels(mResolver, mInputId, existingChannels);
        String language = SetupUtils.getLanguageISO3(mContext);

        new Channels().load(mConnection, language, new Channels.Callback() {
            @Override
            public boolean onChannel(final Channel entry) {
                final Uri uri = existingChannels.get(entry.getNumber());

                // exit if task is cancelled
                if(isCancelled()) {
                    return false;
                }

                try {
                    if(uri != null) {
                        fetchChannelLogo(uri, entry.getIconURL());
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

        return null;
    }
}
