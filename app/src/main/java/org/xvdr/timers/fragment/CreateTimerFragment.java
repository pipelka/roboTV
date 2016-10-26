package org.xvdr.timers.fragment;

import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.Timer;
import org.xvdr.robotv.setup.SetupUtils;

import java.util.List;

public class CreateTimerFragment extends CreateTimerStepFragment {

    static final String TAG = "CreateTimerFragment";

    static final int ACTION_FOLDER = 3;
    static final int ACTION_ADD = 1;
    static final int ACTION_CANCEL = 2;

    private GuidedAction mActionFolder;
    private GuidedAction mActionAdd;
    private GuidedAction mActionCancel;

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.schedule_recording));
    }

    @Override
    public void onCreateActions(List actions, Bundle savedInstanceState) {
        mActionFolder = new GuidedAction.Builder(getActivity())
        .id(ACTION_FOLDER)
        .title(getString(R.string.timer_add_select_folder))
        .build();

        updateFolder();

        mActionAdd = new GuidedAction.Builder(getActivity())
        .id(ACTION_ADD)
        .title(getString(R.string.timer_add_create))
        .hasNext(true)
        .build();

        mActionCancel = new GuidedAction.Builder(getActivity())
        .id(ACTION_CANCEL)
        .title(getString(R.string.timer_add_cancel))
        .build();

        actions.add(mActionFolder);
        actions.add(mActionAdd);
        actions.add(mActionCancel);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFolder();
    }

    protected void updateFolder() {
        String folder = SetupUtils.getRecordingFolder();

        if(folder.isEmpty()) {
            mActionFolder.setDescription(getActivity().getString(R.string.empty_folder));
        }
        else {
            mActionFolder.setDescription(folder);
        }
    }

    protected void createTimer(Movie movie) {
        Connection connection = new Connection("roboTV:createTimer");

        if(!connection.open(SetupUtils.getServer(getActivity()))) {
            return;
        }

        Timer timer = new Timer(connection);

        String name = movie.getCategory();

        if(!name.isEmpty()) {
            name += "~";
        }

        name += movie.getTitle();

        timer.create(movie.getChannelUid(), movie.getStartTime(), (int)movie.getDuration(), name);
        connection.close();
    }

    public void onGuidedActionClicked(GuidedAction action) {
        Movie movie = getMovie();

        switch((int)action.getId()) {
            case ACTION_FOLDER:
                GuidedStepFragment fragment = new CreateTimerFragmentFolder();
                Bundle bundle = new Bundle();
                bundle.putSerializable(EXTRA_MOVIE, movie);
                fragment.setArguments(bundle);
                GuidedStepFragment.add(getActivity().getFragmentManager(), fragment);
                break;

            case ACTION_ADD:
                movie.setCategory(SetupUtils.getRecordingFolder());
                createTimer(movie);
                finishGuidedStepFragments();
                break;

            case ACTION_CANCEL:
                finishGuidedStepFragments();
                break;
        }
    }
}
