package org.xvdr.robotv.setup;

import android.app.Activity;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.xvdr.robotv.R;
import org.xvdr.robotv.syncadapter.SyncUtils;
import org.xvdr.sync.ChannelSyncAdapter;
import org.xvdr.robotv.tv.ServerConnection;

public class SetupActivity extends Activity implements ChannelSyncAdapter.ProgressCallback {
    private static final String TAG = "Setup";

    private String mInputId;
    private ServerConnection mConnection;
    private ProgressBar mProgress;
    private TextView mTextImport;
    private EditText mServer;
    private Spinner mLanguageSpinner;

    private ChannelSyncAdapter channelSync;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);

        setContentView(R.layout.channelimport);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mTextImport = (TextView) findViewById(R.id.textImportChannels);

        Button mButtonImport = (Button) findViewById(R.id.btnImport);
        mButtonImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetupUtils.setServer(SetupActivity.this, mServer.getText().toString());
                SetupActivity.this.registerChannels();
            }
        });

        Button mButtonCancel = (Button) findViewById(R.id.btnCancel);
        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetupActivity.this.finish();
            }
        });

        mServer = (EditText) findViewById(R.id.server);
        mServer.setText(SetupUtils.getServer(this));

        final String[] isoCodeArray = getResources().getStringArray(R.array.iso639_code1);

        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(this, R.array.languages_array, android.R.layout.simple_spinner_item);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mLanguageSpinner = (Spinner) findViewById(R.id.language);
        mLanguageSpinner.setAdapter(languageAdapter);
        mLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SetupUtils.setLanguage(SetupActivity.this, isoCodeArray[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // set current language
        mLanguageSpinner.post(new Runnable() {
            @Override
            public void run() {
                mLanguageSpinner.setSelection(SetupUtils.getLanguageIndex(SetupActivity.this));
            }
        });

        // refresh rate
        String[] array = getResources().getStringArray(R.array.refresh_rate_value_array);

        final float[] refreshRateValueArray = new float[array.length];
        for(int i = 0; i < array.length; i++) {
            refreshRateValueArray[i] = Float.parseFloat(array[i]);
        }

        ArrayAdapter<CharSequence> refreshRateAdapter = ArrayAdapter.createFromResource(this, R.array.refresh_rate_name_array, android.R.layout.simple_spinner_item);
        refreshRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final Spinner refreshRateSpinner = (Spinner) findViewById(R.id.refresh_rate);
        refreshRateSpinner.setAdapter(refreshRateAdapter);
        refreshRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "set refresh rate: " + refreshRateValueArray[i]);
                SetupUtils.setRefreshRate(SetupActivity.this, refreshRateValueArray[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // set current refresh rate
        refreshRateSpinner.post(new Runnable() {
            @Override
            public void run() {
                refreshRateSpinner.setSelection(SetupUtils.getRefreshRateIndex(SetupActivity.this));
            }
        });

        Log.i(TAG, "creating XVDR connection ...");
        mConnection = new ServerConnection("AndroidTV Settings");
    }

    private boolean registerChannels() {
        channelSync = new ChannelSyncAdapter(this, mInputId, mConnection);
        String server = SetupUtils.getServer(SetupActivity.this);

        mConnection.close();
        if(!mConnection.open(server)) {
            Toast.makeText(this, getString(R.string.connect_unable), Toast.LENGTH_SHORT).show();
            return false;
        }

        mProgress.setVisibility(View.VISIBLE);
        mTextImport.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                channelSync.setProgressCallback(SetupActivity.this);
                channelSync.syncChannels(true);
                mConnection.close();

                SyncUtils.setUpPeriodicSync(SetupActivity.this, mInputId);
                SyncUtils.requestSync(mInputId);
            }
        }).start();

        return true;
    }

    @Override
    public void onProgress(int done, int total) {
        mProgress.setMax(total);
        mProgress.setProgress(done);
    }

    @Override
    public void onDone() {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onCancel() {
        finish();
    }

}
