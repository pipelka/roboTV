package org.xvdr.robotv.setup;

import android.app.Activity;
import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.util.Log;
import android.widget.Toast;

import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.syncadapter.SyncUtils;
import org.xvdr.sync.ChannelSyncAdapter;
import org.xvdr.robotv.client.Connection;

public class SetupActivity extends Activity {
    private static final String TAG = "Setup";

    private String mInputId;
    private Connection mConnection;

    private ChannelSyncAdapter channelSync;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);

        Log.i(TAG, "creating roboTV connection ...");
        mConnection = new Connection("AndroidTV Settings");

        GuidedStepFragment.addAsRoot(this, new SetupFragment(), android.R.id.content);
    }

    String getInputId() {
        return mInputId;
    }

    public boolean registerChannels(final ChannelSyncAdapter.ProgressCallback progress) {
        // reconnect data service
        Intent serviceIntent = new Intent(this, DataService.class);
        startService(serviceIntent);

        // sync channels
        channelSync = new ChannelSyncAdapter(this, mInputId, mConnection);
        String server = SetupUtils.getServer(SetupActivity.this);

        mConnection.close();

        if(!mConnection.open(server)) {
            Toast.makeText(this, getString(R.string.connect_unable), Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                channelSync.setProgressCallback(progress);
                channelSync.syncChannels(true);
                mConnection.close();

                SyncUtils.setUpPeriodicSync(SetupActivity.this, mInputId);
                SyncUtils.requestSync(mInputId);
            }
        }).start();

        return true;
    }
}
