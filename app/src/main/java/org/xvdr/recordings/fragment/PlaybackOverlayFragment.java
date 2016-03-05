package org.xvdr.recordings.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.xvdr.recordings.activity.PlayerActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.presenter.DetailsDescriptionPresenter;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment {

    /* For cardImage loading to playbackRow */
    public class PicassoPlaybackControlsRowTarget implements Target {
        PlaybackControlsRow mPlaybackControlsRow;

        public PicassoPlaybackControlsRowTarget(PlaybackControlsRow playbackControlsRow) {
            mPlaybackControlsRow = playbackControlsRow;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            mPlaybackControlsRow.setImageBitmap(PlaybackOverlayFragment.this.getActivity(), bitmap);
            mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            mPlaybackControlsRow.setImageDrawable(drawable);
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
            // Do nothing, default_background manager has its own transitions
        }
    }

    private static final String TAG = PlaybackOverlayFragment.class.getSimpleName();
    private static final int SIMULATED_BUFFERED_TIME = 10000;
    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int UPDATE_PERIOD = 16;

    private Movie mSelectedMovie;
    private int mCurrentPlaybackState;
    private Handler mHandler;
    private Runnable mRunnable;

    private PlaybackControlsRow mPlaybackControlsRow;
    private ArrayObjectAdapter mPrimaryActionAdapter;
    private ArrayObjectAdapter mSecondaryActionAdapter;

    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;
    /*private PlaybackControlsRow.HighQualityAction mHighQualityAction;
    private PlaybackControlsRow.RepeatAction mRepeatAction;
    private PlaybackControlsRow.ThumbsUpAction mThumbsUpAction;
    private PlaybackControlsRow.ThumbsDownAction mThumbsDownAction;
    private PlaybackControlsRow.ShuffleAction mShuffleAction;
    private PlaybackControlsRow.ClosedCaptioningAction mClosedCaptioningAction;
    private PlaybackControlsRow.MoreActions mMoreActions;*/

    private PicassoPlaybackControlsRowTarget mPlaybackControlsRowTarget;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSelectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);
        mHandler = new Handler(Looper.getMainLooper());

        setBackgroundType(PlaybackOverlayFragment.BG_LIGHT);
        setFadingEnabled(false);

        setUpRows();
    }

    private ArrayObjectAdapter mRowsAdapter;

    private void setUpRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        playbackControlsRowPresenter = new PlaybackControlsRowPresenter(new DetailsDescriptionPresenter());

        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        /*
         * Add PlaybackControlsRow to mRowsAdapter, which makes video control UI.
         * PlaybackControlsRow is supposed to be first Row of mRowsAdapter.
         */
        addPlaybackControlsRow();
        /* add ListRow to second row of mRowsAdapter */
        addOtherRows();

        /* onClick */
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if(action.getId() == mPlayPauseAction.getId()) {
                    /* PlayPause action */
                    togglePlayback(mPlayPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.PLAY);
                }
                else if(action.getId() == mFastForwardAction.getId()) {
                    /* FastForward action  */
                    fastForward(60 * 1000);
                }
                else if(action.getId() == mRewindAction.getId()) {
                    /* Rewind action */
                    rewind(60 * 1000);
                }
                else if(action.getId() == mSkipNextAction.getId()) {
                    fastForward(10 * 60 * 1000);
                }
                else if(action.getId() == mSkipPreviousAction.getId()) {
                    rewind(10 * 60 * 1000);
                }

                if(action instanceof PlaybackControlsRow.MultiAction) {
                    /* Following action is subclass of MultiAction
                     * - PlayPauseAction
                     * - FastForwardAction
                     * - RewindAction
                     * - ThumbsAction
                     * - RepeatAction
                     * - ShuffleAction
                     * - HighQualityAction
                     * - ClosedCaptioningAction
                     */
                    notifyChanged(action);
                }
            }
        });

        setAdapter(mRowsAdapter);

    }

    protected void updateVideoImage(String url) {
        Log.d(TAG, "load url " + url);

        if(url == null || url.isEmpty()) {
            return;
        }

        Picasso.with(getActivity())
        .load(url)
        .resize(Utils.dpToPx(getActivity().getResources().getInteger(R.integer.detail_thumbnail_square_size), getActivity().getApplicationContext()),
                Utils.dpToPx(getActivity().getResources().getInteger(R.integer.detail_thumbnail_square_height), getActivity().getApplicationContext()))
        .into(mPlaybackControlsRowTarget);
    }

    private void addPlaybackControlsRow() {
        mPlaybackControlsRow = new PlaybackControlsRow(mSelectedMovie);

        mPlaybackControlsRow.setTotalTime((int) mSelectedMovie.getDuration() * 1000);
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(0);

        mPlaybackControlsRowTarget = new PicassoPlaybackControlsRowTarget(mPlaybackControlsRow);

        mRowsAdapter.add(mPlaybackControlsRow);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionAdapter = new ArrayObjectAdapter(presenterSelector);
        mSecondaryActionAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionAdapter);

        Activity activity = getActivity();
        mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(activity);
        /*mRepeatAction = new PlaybackControlsRow.RepeatAction(activity);
        mThumbsUpAction = new PlaybackControlsRow.ThumbsUpAction(activity);
        mThumbsDownAction = new PlaybackControlsRow.ThumbsDownAction(activity);
        mShuffleAction = new PlaybackControlsRow.ShuffleAction(activity);*/
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(activity);
        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(activity);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(activity);
        mRewindAction = new PlaybackControlsRow.RewindAction(activity);
        /*mHighQualityAction = new PlaybackControlsRow.HighQualityAction(activity);
        mClosedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(activity);
        mMoreActions = new PlaybackControlsRow.MoreActions(activity);*/

        /* PrimaryAction setting */
        mPrimaryActionAdapter.add(mSkipPreviousAction);
        mPrimaryActionAdapter.add(mRewindAction);
        mPrimaryActionAdapter.add(mPlayPauseAction);
        mPrimaryActionAdapter.add(mFastForwardAction);
        mPrimaryActionAdapter.add(mSkipNextAction);

        /* SecondaryAction setting */
        /*mSecondaryActionAdapter.add(mThumbsUpAction);
        mSecondaryActionAdapter.add(mThumbsDownAction);
        mSecondaryActionAdapter.add(mRepeatAction);
        mSecondaryActionAdapter.add(mShuffleAction);
        mSecondaryActionAdapter.add(mHighQualityAction);
        mSecondaryActionAdapter.add(mClosedCaptioningAction);
        mSecondaryActionAdapter.add(mMoreActions);*/

        updateVideoImage(mSelectedMovie.getCardImageUrl());
    }

    private int getUpdatePeriod() {
        return DEFAULT_UPDATE_PERIOD;
    }

    private void startProgressAutomation() {
        if(mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    int updatePeriod = getUpdatePeriod();
                    int currentTime = getCurrentTime();
                    int totalTime = mPlaybackControlsRow.getTotalTime();

                    if(currentTime <= totalTime) {
                        mPlaybackControlsRow.setCurrentTime(currentTime);
                        mPlaybackControlsRow.setBufferedProgress(currentTime + SIMULATED_BUFFERED_TIME);
                    }

                    ((PlayerActivity)getActivity()).updatePlaybackState();

                    mHandler.postDelayed(this, updatePeriod);
                }
            };
            mHandler.postDelayed(mRunnable, getUpdatePeriod());
        }
    }

    public void stopProgressAutomation() {
        if(mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mRunnable = null;
        }
    }

    public void playbackStateChanged() {

        if(mCurrentPlaybackState != PlaybackState.STATE_PLAYING) {
            mCurrentPlaybackState = PlaybackState.STATE_PLAYING;
            startProgressAutomation();
            setFadingEnabled(true);
            mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PAUSE);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.PAUSE));
            notifyChanged(mPlayPauseAction);
        }
        else if(mCurrentPlaybackState != PlaybackState.STATE_PAUSED) {
            mCurrentPlaybackState = PlaybackState.STATE_PAUSED;
            stopProgressAutomation();
            setFadingEnabled(false); // if set to false, PlaybackcontrolsRow will always be on the screen
            mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PLAY);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.PLAY));
            notifyChanged(mPlayPauseAction);
        }

        int currentTime = getCurrentTime();
        mPlaybackControlsRow.setCurrentTime(currentTime);
        mPlaybackControlsRow.setBufferedProgress(currentTime + SIMULATED_BUFFERED_TIME);

    }

    public int getPlaybackState() {
        return mCurrentPlaybackState;
    }

    private int getCurrentTime() {
        PlayerActivity activity = (PlayerActivity) getActivity();

        if(activity == null) {
            return 0;
        }

        return activity.getCurrentTime();
    }
    private void notifyChanged(Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionAdapter;

        if(adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }

        adapter = mSecondaryActionAdapter;

        if(adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
    }

    private void fastForward(int timeMs) {
        ((PlayerActivity) getActivity()).fastForward(timeMs);
    }

    private void rewind(int timeMs) {
        ((PlayerActivity) getActivity()).rewind(timeMs);
    }

    private void addOtherRows() {
        /*ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        Movie movie = new Movie();
        movie.setTitle("Title");
        movie.setStudio("studio");
        movie.setDescription("description");
        movie.setCardImageUrl("http://heimkehrend.raindrop.jp/kl-hacker/wp-content/uploads/2014/08/DSC02580.jpg");
        listRowAdapter.add(movie);
        listRowAdapter.add(movie);

        HeaderItem header = new HeaderItem(0, "OtherRows");
        mRowsAdapter.add(new ListRow(header, listRowAdapter));*/
    }


    public void togglePlayback(boolean playPause) {
        ((PlayerActivity) getActivity()).playPause(playPause);
        playbackStateChanged();
    }
}
