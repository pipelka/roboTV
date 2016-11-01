package org.xvdr.timers.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.NotificationHandler;
import org.xvdr.robotv.setup.SetupUtils;

import java.util.List;

public class CreateTimerFragment extends CreateTimerStepFragment {

    static final int ACTION_FOLDER = 3;
    static final int ACTION_ADD = 1;
    static final int ACTION_CANCEL = 2;

    private GuidedAction mActionFolder;
    private GuidedAction mActionAdd;
    private GuidedAction mActionCancel;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.schedule_recording));
    }

    @Override
    public void onCreateActions(@NonNull List actions, Bundle savedInstanceState) {
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
        DataService service = getService();
        NotificationHandler notificationHandler = new NotificationHandler(getActivity());

        if(service == null) {
            notificationHandler.error(getString(R.string.service_not_connected));
            return;
        }

        if(service.createTimer(movie)) {
            notificationHandler.notify(getString(R.string.timer_created), movie.getTitle(), getDrawable());
            return;
        }

        notificationHandler.error(getString(R.string.failed_create_timer));
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        Movie movie = getMovie();

        switch((int)action.getId()) {
            case ACTION_FOLDER:
                CreateTimerFragmentFolder fragment = new CreateTimerFragmentFolder();
                fragment.startGuidedStep(getActivity(), movie, getService(), getResourceId());
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
