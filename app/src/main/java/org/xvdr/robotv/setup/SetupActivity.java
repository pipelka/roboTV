package org.xvdr.robotv.setup;

import android.app.Activity;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.util.Log;
import android.widget.Toast;

import org.xvdr.robotv.R;
import org.xvdr.robotv.syncadapter.SyncUtils;
import org.xvdr.sync.ChannelSyncAdapter;
import org.xvdr.robotv.tv.ServerConnection;

public class SetupActivity extends Activity {
    private static final String TAG = "Setup";

    private String mInputId;
    private ServerConnection mConnection;

    private ChannelSyncAdapter channelSync;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);

        Log.i(TAG, "creating XVDR connection ...");
        mConnection = new ServerConnection("AndroidTV Settings");

        GuidedStepFragment.addAsRoot(this, new SetupFragmentRoot(), android.R.id.content);
    }

    public boolean registerChannels(final ChannelSyncAdapter.ProgressCallback progress) {
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
