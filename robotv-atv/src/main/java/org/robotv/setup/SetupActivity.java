package org.robotv.setup;

import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;

import android.util.Log;
import android.widget.Toast;

import org.robotv.dataservice.DataServiceClient;
import org.robotv.robotv.R;
import org.robotv.dataservice.DataService;
import org.robotv.sync.ChannelSyncAdapter;
import org.robotv.client.Connection;

public class SetupActivity extends FragmentActivity {
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

        GuidedStepSupportFragment.addAsRoot(this, new SetupFragment(), android.R.id.content);
    }

    String getInputId() {
        return mInputId;
    }

    public void registerChannels(final ChannelSyncAdapter.ProgressCallback progress) {
        // sync channels
        channelSync = new ChannelSyncAdapter(mConnection, this, mInputId);
        SetupUtils.setInputId(this, mInputId);

        String server = SetupUtils.getServer(SetupActivity.this);

        if(!mConnection.open(server)) {
            Toast.makeText(this, getString(R.string.connect_unable), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DataService.cancelSyncJob(this);

        new Thread(() -> {
            channelSync.setProgressCallback(progress);
            channelSync.syncChannels(true);
            //channelSync.syncChannels(false);
            mConnection.close();

            DataService.scheduleSyncJob(this, true);

            // connect data service
            Intent serviceIntent = new Intent(SetupActivity.this, DataService.class);
            startService(serviceIntent);
        }).start();

    }
}
