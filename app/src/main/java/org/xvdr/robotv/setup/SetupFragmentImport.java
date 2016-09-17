package org.xvdr.robotv.setup;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.xvdr.robotv.R;
import org.xvdr.sync.ChannelSyncAdapter;

public class SetupFragmentImport extends Fragment implements ChannelSyncAdapter.ProgressCallback {

    private static final String TAG = SetupFragmentImport.class.getSimpleName();

    protected ProgressBar mProgressBar;
    protected Handler mHandler;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mHandler = new Handler();

        return view;
    }

    @Override
    public void onProgress(final int done, final int total) {
        if(mProgressBar == null) {
            return;
        }

        mProgressBar.setMax(total);
        mProgressBar.setProgress(done);
    }

    @Override
    public void onDone() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }
}
