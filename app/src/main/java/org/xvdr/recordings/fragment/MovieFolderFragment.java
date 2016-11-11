package org.xvdr.recordings.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.robotv.client.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.ui.MovieStepFragment;

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

        moveMovie(action.getTitle().toString());
        getActivity().finishAndRemoveTask();
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
    }

}
