package org.xvdr.sync;

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
import android.os.AsyncTask;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.util.Log;

import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.model.Channel;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.Channels;
import org.xvdr.robotv.client.Connection;
import org.xvdr.timers.activity.TimerActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    private ContentResolver resolver;

    private ProgressCallback progressCallback = null;
    private SyncChannelIconsTask channelIconsTask = null;

    private final ThreadPoolExecutor poolExecutorEPG;

    private static final int CORE_POOL_SIZE = 6;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_SECONDS = 30;

    public ChannelSyncAdapter(Connection connection, Context context, String inputId) {
        this.context = context;
        this.connection = connection;
        this.inputId = inputId;
        this.resolver = context.getContentResolver();

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

    public void syncChannels() {
        Log.i(TAG, "syncing channel list ...");

        // fetch existing channel list
        LinkedHashMap<Integer, Uri> existingChannels = new LinkedHashMap<>();
        getExistingChannels(resolver, this.inputId, existingChannels);

        // update or insert channels

        Channels list = new Channels();
        String language = SetupUtils.getLanguageISO3(context);
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        list.load(connection, language);

        int i = 0;

        Log.d(TAG, String.format("syncing %d channels", list.size()));

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

            Uri channelUri = existingChannels.get(entry.getNumber());

            // insert new channel
            if(channelUri == null) {
                Log.d(TAG, String.format("adding new channel %d - %s", entry.getNumber(), entry.getName()));
                ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI).withValues(values).build());
            }
            // update existing channel
            else {
                Log.d(TAG, String.format("updating channel %d - %s", entry.getNumber(), entry.getName()));
                ops.add(ContentProviderOperation.newUpdate(channelUri).withValues(values).build());
                existingChannels.remove(entry.getNumber());
            }

            if((i % 100) == 0) {
                Log.d(TAG, "batch commiting changes");

                try {
                    context.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(TAG, "batch operation failed !");
                }

                ops.clear();
            }

            if (progressCallback != null) {
                progressCallback.onProgress(i, list.size());
            }

            i++;
        }

        // remove orphaned channels

        Log.d(TAG, String.format("removing %d orphaned channels", existingChannels.size()));

        for(LinkedHashMap.Entry<Integer, Uri> pair : existingChannels.entrySet()) {
            ops.add(ContentProviderOperation.newDelete(pair.getValue()).build());
        }

        // push pending operations

        Log.d(TAG, "batch commiting final changes");
        try {
            context.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
        }
        catch(RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Failed to update EPG for channel !", e);
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
        LinkedHashMap<Integer, Uri> existingChannels = new LinkedHashMap<>();
        getExistingChannels(resolver, this.inputId, existingChannels);

        Log.i(TAG, String.format("syncing epg for %d channels...", existingChannels.size()));

        // fetch epg entries for each channel
        for(LinkedHashMap.Entry<Integer, Uri> pair : existingChannels.entrySet()) {
            Log.d(TAG, String.format("importing EPG of channel %d", pair.getKey()));
            SyncChannelEPGTask task = new SyncChannelEPGTask(connection, context, true);
            task.executeOnExecutor(poolExecutorEPG, pair.getValue());
        }
    }

    static void getExistingChannels(ContentResolver resolver, String inputId, LinkedHashMap<Integer, Uri> existingChannels) {
        // Create a map from original network ID to channel row ID for existing channels.
        existingChannels.clear();

        Uri channelUri = TvContract.buildChannelsUriForInput(inputId);
        String[] projection = {TvContract.Channels._ID, TvContract.Channels.COLUMN_DISPLAY_NUMBER};

        Cursor cursor = null;

        try {
            cursor = resolver.query(channelUri, projection, null, null, null);

            while(cursor != null && cursor.moveToNext()) {
                long channelId = cursor.getLong(0);
                int number = cursor.getInt(1);
                existingChannels.put(number, TvContract.buildChannelUri(channelId));
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
