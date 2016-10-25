package org.xvdr.timers.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

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
            try {
                Bitmap bitmap = Glide.with(getActivity())
                    .load(url).asBitmap()
                    .into(
                        Utils.dp(R.integer.artwork_background_width, activity),
                        Utils.dp(R.integer.artwork_background_height, activity)).get();

                mDrawable = new BitmapDrawable(getResources(), bitmap);
            }
            catch(Exception e) {
                mDrawable = getActivity().getDrawable(R.drawable.recording_unkown);
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
