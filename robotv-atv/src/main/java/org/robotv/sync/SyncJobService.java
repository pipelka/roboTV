package org.robotv.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.robotv.client.Connection;
import org.robotv.setup.SetupUtils;

public class SyncJobService extends JobService {

    public static final String TAG = "SyncJobService";

    static class SyncTask extends AsyncTask<Void, Void, Boolean> {

        private final ChannelSyncAdapter adapter;

        SyncTask(ChannelSyncAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            adapter.reset();

            if(!isCancelled()) {
                adapter.syncChannels(false);
            }

            if(!isCancelled()) {
                adapter.syncEPG();
            }

            if(!isCancelled()) {
                adapter.syncChannelIcons();
            }

            return !isCancelled();
        }

        void cancelJob() {
            adapter.cancel();
        }
    }

    SyncTask syncTask;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        String inputId = SetupUtils.getInputId(this);

        Connection connection = new Connection("roboTV Sync Adapter");
        connection.setPriority(1);

        Context context = getApplication();

        if(!connection.open(SetupUtils.getServer(context))) {
            Log.e(TAG, "unable to connect to server");
            return false;
        }

        ChannelSyncAdapter syncAdapter = new ChannelSyncAdapter(connection, context, inputId);

        syncTask = new SyncTask(syncAdapter) {

            @Override
            protected void onPostExecute(Boolean success) {
                jobFinished(jobParameters, !success);
            }

        };

        syncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if(syncTask != null) {
            syncTask.cancelJob();
            syncTask.cancel(true);
        }

        return true;
    }
}
