package org.xvdr.recordings.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.xvdr.robotv.R;

public class SpinnerFragment extends Fragment {

    private static final String TAG = SpinnerFragment.class.getSimpleName();

    private static final int SPINNER_WIDTH = 200;
    private static final int SPINNER_HEIGHT = 200;

    protected ProgressBar mProgressBar;
    protected TextView mText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spinner, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(SPINNER_WIDTH, SPINNER_HEIGHT, Gravity.CENTER);
        mProgressBar.setLayoutParams(layoutParams);

        mText = (TextView) view.findViewById(R.id.text);

        return view;
    }
}
