package org.robotv.recordings.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.robotv.client.model.Movie;
import org.robotv.robotv.R;
import org.robotv.ui.MovieStepFragment;

import java.util.List;
import java.util.TreeSet;

public class MovieFolderFragment extends MovieStepFragment {

    protected TreeSet<String> folderList;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.movie_move_to_folder));
    }

    @Override
    public void onCreateActions(@NonNull List actions, Bundle savedInstanceState) {
        folderList = getMovieController().getFolderList();

        actions.add(new GuidedAction.Builder(getActivity())
                .id(0)
                .title(getString(R.string.cancel))
                .icon(R.drawable.baseline_close_white_48dp)
                .build());

        int i = 1;

        for(String folder : folderList) {
            GuidedAction action = new GuidedAction.Builder(getActivity())
            .id(i++)
            .title(folder)
            .build();

            actions.add(action);
        }

        String newFolder = getString(R.string.timer_new_folder);

        GuidedAction customFolder = new GuidedAction.Builder(getActivity())
            .id(i)
            .title(newFolder)
            .icon(R.drawable.ic_add_circle_outline_white_48dp)
            .description(getString(R.string.timer_add_create_new_folder))
            .editable(true)
            .build();

        actions.add(customFolder);

    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if(action.getId() == 0) {
            getFragmentManager().popBackStack();
            return;
        }

        showProgress();

        new Thread(() -> {
            moveMovie(action.getTitle().toString());
            post(() -> {
                hideProgress();
                finishGuidedStepSupportFragments();
            });

        }).start();
    }

    private String mapName(String name) {
        return name.replace(' ', '_').replace(':', '_');
    }

    private void moveMovie(String folder) {
        String name ;
        Movie movie = getMovie();

        if(movie.isTvShow() || folder.equals(getService().getSeriesFolder())) {
            name = folder + "~" + movie.getTitle() + "~" + movie.getShortText();
        }
        else {
            name = folder + "~" + movie.getTitle();
        }

        String newName = mapName(name);
        getMovieController().renameMovie(movie, newName);
        getService().triggerMovieUpdate();
    }

}
