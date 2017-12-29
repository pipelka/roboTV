package org.robotv.timers.fragment;

import android.text.TextUtils;

import org.robotv.ui.MovieStepFragment;
import org.robotv.client.model.Movie;

import org.robotv.robotv.R;

public class CreateTimerStepFragment extends MovieStepFragment {

    protected String onCreateDescription(Movie movie) {
        return getString(
                R.string.timer_add_desc,
                (TextUtils.isEmpty(movie.getShortText()) ? "" : movie.getShortText() + " / "),
                movie.getChannelName(),
                movie.getDateTime());
    }

}
