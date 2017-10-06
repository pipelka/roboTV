package org.xvdr.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.model.Channel;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.Channels;
import org.xvdr.robotv.client.Connection;
import org.xvdr.timers.activity.TimerActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChannelSyncAdapter {

    public interface ProgressCallback {

        void onProgress(int done, int total);

        void onDone();
    }

    private static final String TAG = "ChannelSyncAdapter";

    private Context context;
    private Connection connection;
    private String inputId;

    private final SparseArray<Long> existingChannels = new SparseArray<>();
    private ContentResolver resolver;

    private ProgressCallback progressCallback = null;
    private SyncChannelIconsTask channelIconsTask = null;

    private final ThreadPoolExecutor poolExecutorEPG;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = 6; //Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = 10; //CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;

    public ChannelSyncAdapter(Connection connection, Context context, String inputId) {
        this.context = context;
        this.connection = connection;
        this.inputId = inputId;
        this.resolver = context.getContentResolver();

        // fetch existing channel list
        getExistingChannels(resolver, this.inputId, existingChannels);

        // create pool executor
        poolExecutorEPG = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                Executors.defaultThreadFactory());
    }

    public void setProgressCallback(ProgressCallback callback) {
        progressCallback = callback;
    }

    public void syncChannels(boolean removeExisting) {
        Log.i(TAG, "syncing channel list ...");

        // remove existing channels
        if(removeExisting)  {
            Uri uri = TvContract.buildChannelsUriForInput(inputId);
            resolver.delete(uri, null, null);
        }

        // update or insert channels

        Channels list = new Channels();
        String language = SetupUtils.getLanguageISO3(context);

        list.load(connection, language);

        int i = 0;

        for(Channel entry : list) {

            // skip obsolete channels
            if(entry.getName().endsWith("OBSOLETE")) {
                continue;
            }

            // epg search intent

            Intent intent = new Intent(context, TimerActivity.class);
            intent.putExtra("uid", entry.getUid());
            intent.putExtra("name", entry.getName());

            String link = "intent:" + intent.toUri(0);

            Uri channelUri;
            Long channelId = existingChannels.get(entry.getUid());

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

            // channel link needs Android M
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                values.put(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI, getUriToResource(context, R.drawable.banner_timers).toString());
                values.put(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI, link);
                values.put(TvContract.Channels.COLUMN_APP_LINK_TEXT, context.getString(R.string.timer_title));
                values.put(TvContract.Channels.COLUMN_APP_LINK_COLOR, Utils.getColor(context, R.color.primary_color));
                values.put(TvContract.Channels.COLUMN_APP_LINK_ICON_URI, "");
            }

            // insert new channel
            if(channelId == null) {
                resolver.insert(TvContract.Channels.CONTENT_URI, values);
            }
            // update existing channel
            else {
                channelUri = TvContract.buildChannelUri(channelId);

                if(channelUri != null) {
                    resolver.update(channelUri, values, null, null);
                    existingChannels.remove(entry.getUid());
                }
            }

            if(progressCallback != null) {
                progressCallback.onProgress(++i, list.size());
            }

        }

        // remove orphaned channels

        int size = existingChannels.size();

        for(i = 0; i < size; ++i) {
            Long channelId = existingChannels.valueAt(i);

            if(channelId == null) {
                continue;
            }

            Uri uri = TvContract.buildChannelUri(channelId);
            resolver.delete(uri, null, null);
        }

        if(progressCallback != null) {
            progressCallback.onDone();
        }

        Log.i(TAG, "synced channels");
    }

    public void syncChannelIcons() {
        // task already running
        if(channelIconsTask != null) {
            return;
        }

        Log.i(TAG, "syncing of channel icons started.");

        channelIconsTask = new SyncChannelIconsTask(connection, context, inputId) {
            @Override
            protected void onPostExecute(Void result) {
                channelIconsTask = null;
                Log.i(TAG, "finished syncing channel icons.");
            }

            @Override
            protected void onCancelled(Void result) {
                channelIconsTask = null;
                Log.i(TAG, "syncing of channel icons cancelled.");
            }
        };

        channelIconsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void syncEPG() {
        Log.i(TAG, "syncing epg ...");

        // fetch epg entries for each channel

        for(int i = 0; i < existingChannels.size(); ++i) {

            SyncChannelEPGTask task = new SyncChannelEPGTask(connection, context, true);
            Uri channelUri = TvContract.buildChannelUri(existingChannels.valueAt(i));

            task.executeOnExecutor(poolExecutorEPG, channelUri);
        }

        //Log.i(TAG, "synced schedule for " + existingChannels.size() + " channels");
    }

    static void getExistingChannels(ContentResolver resolver, String inputId, SparseArray<Long> existingChannels) {
        // Create a map from original network ID to channel row ID for existing channels.
        existingChannels.clear();

        Uri channelUri = TvContract.buildChannelsUriForInput(inputId);
        String[] projection = {TvContract.Channels._ID, TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, TvContract.Channels.COLUMN_DISPLAY_NUMBER};

        Cursor cursor = null;

        try {
            cursor = resolver.query(channelUri, projection, null, null, TvContract.Channels.COLUMN_DISPLAY_NUMBER);

            while(cursor != null && cursor.moveToNext()) {
                long channelId = cursor.getLong(0);
                int uid = cursor.getInt(1);
                existingChannels.put(uid, channelId);
            }
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    private static Uri getUriToResource(@NonNull Context context, @AnyRes int resId) throws Resources.NotFoundException {
        Resources res = context.getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                         "://" + res.getResourcePackageName(resId)
                         + '/' + res.getResourceTypeName(resId)
                         + '/' + res.getResourceEntryName(resId));
    }
}
