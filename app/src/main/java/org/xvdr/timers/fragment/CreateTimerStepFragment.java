package org.xvdr.timers.fragment;

import android.text.TextUtils;

import org.xvdr.ui.MovieStepFragment;
import org.xvdr.recordings.model.Movie;

import org.xvdr.robotv.R;

public class CreateTimerStepFragment extends MovieStepFragment {

    protected String onCreateDescription(Movie movie) {
        return getString(
                R.string.timer_add_desc,
                (TextUtils.isEmpty(movie.getShortText()) ? "" : movie.getShortText() + " / "),
                movie.getChannelName(),
                movie.getDateTime());
    }

}
