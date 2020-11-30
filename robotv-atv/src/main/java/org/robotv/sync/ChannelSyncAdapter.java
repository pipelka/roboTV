package org.robotv.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.model.Channel;
import org.robotv.setup.SetupActivity;
import org.robotv.setup.SetupUtils;
import org.robotv.client.Channels;
import org.robotv.client.Connection;
import org.robotv.timers.activity.TimerActivity;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChannelSyncAdapter {

    public interface ProgressCallback {

        void onProgress(int done, int total);

        void onDone();
    }

    private static final String TAG = "ChannelSyncAdapter";

    private final Context context;
    private final Connection connection;
    private final String inputId;
    private final ContentResolver resolver;
    private final OkHttpClient client;
    private ProgressCallback progressCallback = null;
    private boolean cancelled = false;

    public ChannelSyncAdapter(Connection connection, Context context, String inputId) {
        this.context = context;
        this.connection = connection;
        this.inputId = inputId;
        this.resolver = context.getContentResolver();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    public void setProgressCallback(ProgressCallback callback) {
        progressCallback = callback;
    }

    public void syncChannels(boolean cleanup) {
        // remove all channels (do a total resync)
        if(cleanup) {
            Log.i(TAG, "removing channels ...");
            Uri uri = TvContract.buildChannelsUriForInput(this.inputId);
            resolver.delete(uri, null, null);
        }

        Log.i(TAG, "syncing channel list ...");

        // fetch existing channel list
        SortedMap<Integer, Uri> existingChannels = new TreeMap<>();
        getExistingChannels(resolver, this.inputId, existingChannels);

        // update or insert channels

        Channels list = new Channels();
        String language = SetupUtils.getLanguageISO3(context);

        list.load(connection, language);

        int i = 0;

        Log.d(TAG, String.format("syncing %d channels", list.size()));

        for(Channel entry : list) {

            if(cancelled) {
                return;
            }

            // skip obsolete channels
            if(entry.getName().endsWith("OBSOLETE")) {
                continue;
            }

            // epg search intent

            Intent intent = new Intent(context, TimerActivity.class);
            intent.putExtra("uid", entry.getUid());
            intent.putExtra("name", entry.getName());

            String link = "intent:" + intent.toUri(0);

            // channel entry
            ContentValues values = new ContentValues();
            values.put(TvContract.Channels.COLUMN_INPUT_ID, inputId);

            values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, Integer.toString(entry.getNumber()));
            values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, entry.getName());
            values.put(TvContract.Channels.COLUMN_SERVICE_ID, 0);
            values.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, 0);
            values.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, entry.getUid());
            values.put(TvContract.Channels.COLUMN_SERVICE_TYPE, entry.isRadio() ? TvContract.Channels.SERVICE_TYPE_AUDIO : TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO);
            values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_DVB_S2);
            values.put(TvContract.Channels.COLUMN_SEARCHABLE, 1);
            values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, Integer.toString(entry.getUid()));
            values.put(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI, getUriToResource(context, R.drawable.banner_timers).toString());
            values.put(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI, link);
            values.put(TvContract.Channels.COLUMN_APP_LINK_TEXT, context.getString(R.string.timer_title));
            values.put(TvContract.Channels.COLUMN_APP_LINK_COLOR, Utils.getColor(context, R.color.primary_color));
            values.put(TvContract.Channels.COLUMN_APP_LINK_ICON_URI, "");

            // add/update channel
            Uri channelUri = existingChannels.get(entry.getNumber());

            // insert new channel
            if(channelUri == null) {
                Log.d(TAG, String.format("adding new channel %d - %s", entry.getNumber(), entry.getName()));
                resolver.insert(TvContract.Channels.CONTENT_URI, values);
            }
            // update existing channel
            else {
                Log.d(TAG, String.format("updating channel %d - %s", entry.getNumber(), entry.getName()));
                resolver.update(channelUri, values, null, null);
                existingChannels.remove(entry.getNumber());
            }

            if (progressCallback != null) {
                progressCallback.onProgress(i, list.size());
            }

            i++;
        }

        // remove orphaned channels
        Log.d(TAG, String.format("removing %d orphaned channels", existingChannels.size()));

        for(SortedMap.Entry<Integer, Uri> pair : existingChannels.entrySet()) {
            resolver.delete(pair.getValue(), null, null);
        }

        if(progressCallback != null) {
            progressCallback.onDone();
        }

        Log.i(TAG, "synced channels");
    }

    void syncChannelIcons() {
        // fetch existing channel list
        final SortedMap<Integer, Uri> existingChannels = new TreeMap<>();
        getExistingChannels(resolver, this.inputId, existingChannels);

        String language = SetupUtils.getLanguage(context);

        new Channels().load(connection, language, new Channels.Callback() {
            @Override
            public boolean onChannel(final Channel entry) {

                if(cancelled) {
                    return false;
                }

                final Uri uri = existingChannels.get(entry.getNumber());

                if(uri != null) {
                    fetchChannelLogo(uri, entry.getIconURL());
                }

                return true;
            }
        });
    }

    private void fetchChannelLogo(Uri channelUri, String address) {
        if(TextUtils.isEmpty(address)) {
            return;
        }

        SyncUtils.ChannelHolder holder = new SyncUtils.ChannelHolder();

        if(!SyncUtils.getChannelInfo(resolver, channelUri, holder)) {
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
        Response response;
        try {
            response = client.newCall(request).execute();
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        InputStream in = new BufferedInputStream(response.body().byteStream());
        OutputStream os;
        try {
            os = resolver.openOutputStream(channelLogoUri);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        if(os == null) {
            Log.e(TAG, String.format("error creating logo: %s", channelLogoUri.toString()));
            return;
        }

        int bytes_read;
        byte[] mBuffer = new byte[4096];

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

    void syncEPG() {
        SortedMap<Integer, Uri> existingChannels = new TreeMap<>();
        getExistingChannels(resolver, this.inputId, existingChannels);

        Log.i(TAG, String.format("syncing epg for %d channels...", existingChannels.size()));

        String language = SetupUtils.getLanguage(context);

        // fetch epg entries for each channel
        for(SortedMap.Entry<Integer, Uri> pair : existingChannels.entrySet()) {

            if(cancelled) {
                return;
            }

            Log.d(TAG, String.format("importing EPG of channel %d", pair.getKey()));

            syncChannelEPG(
                    pair.getValue(),
                    language,
                    true);
        }

        Log.i(TAG, String.format("synced %d channels", existingChannels.size()));
    }

    private void syncChannelEPG(Uri channelUri, String language, boolean append) {
        List<ContentValues> programs = new ArrayList<>();

        if(!SyncUtils.fetchEPGForChannel(connection, language, resolver, channelUri, programs, append)) {
            return;
        }

        if(programs.isEmpty()) {
            return;
        }

        // populate database
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        Uri uri = TvContract.buildProgramsUriForChannel(channelUri);

        if(!append) {
            ops.add(ContentProviderOperation.newDelete(uri).build());
        }

        for(ContentValues values : programs) {
            ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI).withValues(values).build());
        }

        try {
            resolver.applyBatch(TvContract.AUTHORITY, ops);
        }
        catch(RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Failed to update EPG for channel !", e);
        }
    }

    private static void getExistingChannels(ContentResolver resolver, String inputId, SortedMap<Integer, Uri> existingChannels) {
        // Create a map from original network ID to channel row ID for existing channels.
        existingChannels.clear();

        Uri channelUri = TvContract.buildChannelsUriForInput(inputId);
        String[] projection = {TvContract.Channels._ID, TvContract.Channels.COLUMN_DISPLAY_NUMBER};

        try (Cursor cursor = resolver.query(channelUri, projection, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                long channelId = cursor.getLong(0);
                int number = Integer.parseInt(cursor.getString(1));
                existingChannels.put(number, TvContract.buildChannelUri(channelId));
            }
        }
    }

    void reset() {
        cancelled = false;
    }

    void cancel() {
        cancelled = true;
    }

    private static Uri getUriToResource(@NonNull Context context, @AnyRes int resId) throws Resources.NotFoundException {
        Resources res = context.getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                         "://" + res.getResourcePackageName(resId)
                         + '/' + res.getResourceTypeName(resId)
                         + '/' + res.getResourceEntryName(resId));
    }
}
