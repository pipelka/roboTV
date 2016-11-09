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
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class SyncChannelIconsTask extends AsyncTask<Void, Void, Void> {

    private Context mContext;
    private String mInputId;
    private Connection mConnection;
    private ContentResolver mResolver;
    private OkHttpClient client;

    final private byte[] mBuffer = new byte[4096];

    public SyncChannelIconsTask(Connection connection, Context context, String inputId) {
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
        Uri channelLogoUri = TvContract.buildChannelLogoUri(channelUri);

        Request request = new Request.Builder().url(address).build();
        Response response = client.newCall(request).execute();

        InputStream in = new BufferedInputStream(response.body().byteStream());
        OutputStream os = mResolver.openOutputStream(channelLogoUri);

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
                try {
                    fetchChannelLogo(uri, entry.iconURL);
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
