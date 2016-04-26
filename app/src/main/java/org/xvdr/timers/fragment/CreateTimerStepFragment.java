package org.xvdr.timers.fragment;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;

import com.squareup.picasso.Picasso;

import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

import java.io.IOException;

public class CreateTimerStepFragment extends GuidedStepFragment {

    static public final String EXTRA_MOVIE = "extra_movie";

    protected Movie mMovie;
    protected Drawable mDrawable = null;

    protected GuidanceStylist.Guidance createGuidance(String breadCrumb) {
        mMovie = (Movie) getArguments().getSerializable(EXTRA_MOVIE);

        String description = getString(
                                 R.string.timer_add_desc,
                                 (mMovie.getOutline() != null ? mMovie.getOutline() + " / " : ""),
                                 mMovie.getChannelName(),
                                 mMovie.getDateTime());

        final Activity activity = getActivity();
        final String url = mMovie.getBackgroundImageUrl();

        if(url.isEmpty()) {
            mDrawable = getActivity().getDrawable(R.drawable.recording_unkown);
        }
        else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mDrawable = new BitmapDrawable(getResources(), Picasso.with(getActivity())
                                                       .load(url)
                                                       .resize(
                                                           Utils.dp(R.integer.artwork_background_width, activity),
                                                           Utils.dp(R.integer.artwork_background_height, activity))
                                                       .centerCrop()
                                                       .error(activity.getDrawable(R.drawable.recording_unkown))
                                                       .placeholder(activity.getDrawable(R.drawable.recording_unkown))
                                                       .get());
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();

            try {
                t.join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        return new GuidanceStylist.Guidance(
                   mMovie.getTitle(),
                   description,
                   breadCrumb,
                   mDrawable);
    }

}
