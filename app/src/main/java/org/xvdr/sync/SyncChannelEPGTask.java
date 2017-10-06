package org.xvdr.sync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import org.xvdr.robotv.client.Connection;

import java.util.ArrayList;
import java.util.List;

public class SyncChannelEPGTask extends AsyncTask<Uri, Void, Void> {

    static private final String TAG = SyncChannelEPGTask.class.getName();

    private final Connection connection;
    private final Context context;
    private final boolean append;

    public SyncChannelEPGTask(Connection connection, Context context, boolean append) {
        this.connection = connection;
        this.context = context;
        this.append = append;
    }

    private void syncChannelEPG(Uri channelUri) {
        List<ContentValues> programs = new ArrayList<>();

        if(!SyncUtils.fetchEPGForChannel(connection, context, channelUri, programs, append)) {
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
            context.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
        }
        catch(RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Failed to update EPG for channel !", e);
        }
    }


    @Override
    protected Void doInBackground(Uri... params) {
        for(Uri uri: params) {
            syncChannelEPG(uri);
        }
        return null;
    }
}
