package org.xvdr.recordings.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;

import org.xvdr.robotv.R;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.robotv.client.model.Timer;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.NotificationHandler;
import org.xvdr.ui.MovieStepFragment;

import java.util.List;

public class DeleteTimerFragment extends MovieStepFragment {

    static final int ACTION_DELETE = 0;
    static final int ACTION_CANCEL = 1;

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

    private void deleteTimer() {
        NotificationHandler notificationHandler = new NotificationHandler(getActivity());

        if(!getService().getTimerController().deleteTimer(timer.getId())) {
            notificationHandler.error(getString(R.string.failed_delete_timer));
        }
    }

}
