package org.robotv.timers.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.robotv.client.model.Movie;
import org.robotv.robotv.R;
import org.robotv.dataservice.DataService;
import org.robotv.dataservice.NotificationHandler;
import org.robotv.setup.SetupUtils;

import java.util.List;

public class CreateTimerFragment extends CreateTimerStepFragment {

    static final int ACTION_CANCEL = 1;
    static final int ACTION_FOLDER = 2;
    static final int ACTION_ADD = 3;
    static final int ACTION_ADD_SEARCH = 4;

    private GuidedAction mActionFolder;
    private GuidedAction mActionAdd;
    private GuidedAction mActionCancel;
    private GuidedAction mActionSearch;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = createGuidance(getString(R.string.schedule_recording));

        if(getMovie().isTvShow()) {
            mActionFolder.setEnabled(false);
            mActionAdd.setTitle(getString(R.string.timer_add_episode));
        }
        else {
            mActionSearch.setEnabled(false);
        }

        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List actions, Bundle savedInstanceState) {
        mActionFolder = new GuidedAction.Builder(getActivity())
                .id(ACTION_FOLDER)
                .title(getString(R.string.timer_add_select_folder))
                .icon(R.drawable.ic_folder_white_48dp)
                .build();

        updateFolder();

        mActionAdd = new GuidedAction.Builder(getActivity())
                .id(ACTION_ADD)
                .title(getString(R.string.timer_add_create))
                .icon(R.drawable.ic_timer_white_48dp)
                .hasNext(true)
                .build();

        mActionSearch = new GuidedAction.Builder(getActivity())
                .id(ACTION_ADD_SEARCH)
                .title(getString(R.string.timer_add_all_episodes))
                .icon(R.drawable.ic_timetable_white_48dp)
                .hasNext(true)
                .build();

        mActionCancel = new GuidedAction.Builder(getActivity())
                .id(ACTION_CANCEL)
                .title(getString(R.string.timer_add_cancel))
                .icon(R.drawable.baseline_close_white_48dp)
                .build();

        actions.add(mActionCancel);
        actions.add(mActionFolder);
        actions.add(mActionAdd);
        actions.add(mActionSearch);
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

    protected void createTimer(Movie movie, boolean searchTimer) {
        DataService service = getService();
        NotificationHandler notificationHandler = new NotificationHandler(getActivity());

        if(service == null) {
            notificationHandler.error(getString(R.string.service_not_connected));
            return;
        }

        if(searchTimer) {
            if(!service.getTimerController().createSearchTimer(movie)) {
                notificationHandler.error(getString(R.string.failed_create_timer));
                return;
            }

            notificationHandler.notify(movie.getTitle(), getString(R.string.record_all_episodes), movie.getPosterUrl());
            return;
        }

        if(!service.getTimerController().createTimer(movie, service.getSeriesFolder())) {
            notificationHandler.error(getString(R.string.failed_create_timer));
        }
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
                movie.setFolder(SetupUtils.getRecordingFolder());
                createTimer(movie, false);
                finishGuidedStepSupportFragments();
                break;

            case ACTION_ADD_SEARCH:
                createTimer(movie, true);
                finishGuidedStepSupportFragments();
                break;

            case ACTION_CANCEL:
                finishGuidedStepSupportFragments();
                break;
        }
    }
}
