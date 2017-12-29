package org.robotv.recordings.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;

import org.robotv.robotv.R;
import org.robotv.client.model.Movie;
import org.robotv.client.model.Timer;
import org.robotv.dataservice.DataService;
import org.robotv.dataservice.NotificationHandler;
import org.robotv.ui.MovieStepFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class EditTimerFragment extends MovieStepFragment {

    static final int ACTION_DELETE = 0;
    static final int ACTION_CANCEL = 1;
    static final int ACTION_FOLDER = 2;

    private Timer timer;

    public void startGuidedStep(final Activity activity, final Timer timer, DataService service, final int resourceId) {
        this.timer = timer;

        Movie movie = new Movie(timer);
        movie.setPosterUrl(timer.getPosterUrl());

        if(TextUtils.isEmpty(movie.getPosterUrl())) {
            movie.setPosterUrl(timer.getLogoUrl());
        }

        super.startGuidedStep(activity, movie, service, resourceId);
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.delete_timer));
    }

    protected String onCreateDescription(Movie movie) {
        return movie.getShortText();
    }

    @Override
    public void onCreateActions(List actions, Bundle savedInstanceState) {
        List<GuidedAction> subActions = new ArrayList<>();
        TreeSet<String> folderList = getMovieController().getFolderList();

        int i = 0;
        for(String folder : folderList) {
            GuidedAction action = new GuidedAction.Builder(getActivity())
                    .id(i++)
                    .title(folder)
                    .checkSetId(1)
                    .checked(timer.getFolder().equals(folder))
                    .build();

            subActions.add(action);
        }

        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_FOLDER)
                .title(getString(R.string.timer_add_select_folder))
                .description(TextUtils.isEmpty(
                        timer.getFolder()) ? getActivity().getString(R.string.empty_folder) :
                        timer.getFolder())
                .subActions(subActions)
                .build());

        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_CANCEL)
                .title(getString(R.string.cancel))
                .build());

        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_DELETE)
                .title(getString(R.string.delete))
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        switch((int)action.getId()) {
            case ACTION_DELETE:
                deleteTimer();
                getFragmentManager().popBackStack();
                break;
            case ACTION_CANCEL:
                getFragmentManager().popBackStack();
                break;
        }
    }

    public boolean onSubGuidedActionClicked(GuidedAction action) {
        timer.setFolder(action.getTitle().toString());

        GuidedAction a = findActionById(ACTION_FOLDER);
        a.setDescription(action.getTitle());
        notifyActionChanged(findActionPositionById(ACTION_FOLDER));

        updateTimer();
        return true;
    }

    private void updateTimer() {
        NotificationHandler notificationHandler = new NotificationHandler(getActivity());

        if(!getService().getTimerController().updateTimer(timer)) {
            notificationHandler.error(getString(R.string.failed_update_timer));
        }
    }

    private void deleteTimer() {
        NotificationHandler notificationHandler = new NotificationHandler(getActivity());

        if(!getService().getTimerController().deleteTimer(timer.getId())) {
            notificationHandler.error(getString(R.string.failed_delete_timer));
        }
    }

}
