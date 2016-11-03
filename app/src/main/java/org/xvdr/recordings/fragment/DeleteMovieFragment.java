package org.xvdr.recordings.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.service.NotificationHandler;
import org.xvdr.ui.MovieStepFragment;

import java.util.List;

public class DeleteMovieFragment extends MovieStepFragment {

    private NotificationHandler notification;

    static final int ACTION_DELETE = 0;
    static final int ACTION_CANCEL = 1;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.delete_movie));
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
                deleteMovie(getMovie());
                getActivity().finishAndRemoveTask();
                break;
            case ACTION_CANCEL:
                getFragmentManager().popBackStack();
                break;
        }
    }

    private void deleteMovie(Movie movie) {
        int status = getService().deleteMovie(movie);
        if(status == 0) {
            return;
        }

        notification = new NotificationHandler(getActivity());

        switch(status) {
            case Connection.STATUS_RECEIVERS_BUSY:
                notification.notify(getResources().getString(R.string.active_recording));
                break;

            default:
                notification.error(getResources().getString(R.string.failed_delete_movie));
                break;
        }

    }
}
