package org.xvdr.robotv.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.xvdr.robotv.setup.SetupUtils;

import org.xvdr.sync.ChannelSyncAdapter;
import org.xvdr.robotv.client.Connection;

/**
 * A SyncAdapter implementation which updates program info periodically.
 */
class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "SyncAdapter";

    public static final String BUNDLE_KEY_INPUT_ID = "bundle_key_input_id";
    public static final long SYNC_FREQUENCY_SEC = 60 * 60 * 6;  // 6 hours
    private static final int SYNC_WINDOW_SEC = 60 * 60 * 12;  // 12 hours

    private final Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContext = context;
    }

    /**
     * Called periodically by the system in every {@code SYNC_FREQUENCY_SEC}.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync(" + account + ", " + authority + ", " + extras + ")");
        String inputId = extras.getString(SyncAdapter.BUNDLE_KEY_INPUT_ID);

        if(inputId == null) {
            return;
        }

        Connection connection = new Connection("roboTV Sync Adapter");
        connection.setPriority(1);

        if(!connection.open(SetupUtils.getServer(mContext))) {
            Log.e(TAG, "unable to connect to server");
            return;
        }

        ChannelSyncAdapter adapter = new ChannelSyncAdapter(getContext(), inputId, connection);
        adapter.syncChannelIcons();
        adapter.syncChannels(false);
        adapter.syncEPG();

        connection.close();
    }
}
