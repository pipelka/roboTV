package org.robotv.recordings.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.robotv.client.model.Movie;
import org.robotv.robotv.R;
import org.robotv.client.Connection;
import org.robotv.dataservice.NotificationHandler;
import org.robotv.ui.MovieStepFragment;

import java.util.List;

public class DeleteMovieFragment extends MovieStepFragment {

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
                .icon(R.drawable.baseline_close_white_48dp)
                .build());

        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_DELETE)
                .icon(R.drawable.ic_delete_white_48dp)
                .title(getString(R.string.delete))
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        switch((int)action.getId()) {
            case ACTION_DELETE:
                deleteMovie(getMovie());
                break;
            case ACTION_CANCEL:
                getFragmentManager().popBackStack();
                break;
        }
    }

    private void deleteMovie(Movie movie) {
        showProgress();
        final NotificationHandler notification = new NotificationHandler(getActivity());

        new Thread(() -> {
            final int status = getMovieController().deleteMovie(movie);

            post(() -> {
                hideProgress();

                switch(status) {
                    case Connection.STATUS_RECEIVERS_BUSY:
                        notification.notify(activity.getString(R.string.active_recording));
                        break;

                    case Connection.STATUS_SUCCESS:
                        notification.notify(activity.getString(R.string.recording_deleted));
                        break;

                    default:
                        notification.error(activity.getString(R.string.failed_delete_movie));
                        break;
                }

                finishGuidedStepSupportFragments();
            });
        }).start();
    }
}
