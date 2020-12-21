package org.robotv.recordings.fragment;

import android.content.Intent;
import android.os.Bundle;

import org.robotv.recordings.activity.CoverSearchActivity;
import org.robotv.robotv.R;
import org.robotv.ui.MovieStepFragment;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

public class MovieDetailsGuidedStep extends MovieStepFragment {

    static final int ACTION_CANCEL = 0;
    static final int ACTION_POSTER = 1;
    static final int ACTION_FOLDER = 2;
    static final int ACTION_DELETE = 3;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.edit_recording));
    }

    @Override
    public void onCreateActions(List actions, Bundle savedInstanceState) {
        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_CANCEL)
                .title(getString(R.string.cancel))
                .icon(R.drawable.baseline_close_white_48dp)
                .build());

        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_POSTER)
                .title(getString(R.string.change_poster))
                .hasNext(true)
                .icon(R.drawable.ic_style_white_48dp)
                .build());

        if(!getMovie().isTvShow()) {
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(ACTION_FOLDER)
                    .title(getString(R.string.movie_move_to_folder))
                    .description(getMovie().getFolder())
                    .hasNext(true)
                    .icon(R.drawable.ic_folder_white_48dp)
                    .build());
        }

        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_DELETE)
                .title(getString(R.string.delete_movie))
                .hasNext(true)
                .icon(R.drawable.ic_delete_white_48dp)
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        switch((int)action.getId()) {
            case ACTION_CANCEL:
                getFragmentManager().popBackStack();
                break;
            case ACTION_POSTER:
                startCoverActivty();
                finishGuidedStepSupportFragments();
                break;
            case ACTION_FOLDER:
                new MovieFolderFragment().startGuidedStep(getActivity(), getMovie(), getService(), getResourceId());
                break;
            case ACTION_DELETE:
                new DeleteMovieFragment().startGuidedStep(getActivity(), getMovie(), getService(), getResourceId());
                break;
        }
    }

    void startCoverActivty() {
        Intent intent = new Intent(getActivity(), CoverSearchActivity.class);
        intent.putExtra(EXTRA_MOVIE, getMovie());
        startActivityForResult(intent, CoverSearchActivity.REQUEST_COVER);
    }
}
