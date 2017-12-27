package org.xvdr.recordings.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.BaseOnItemViewClickedListener;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.exoplayer2.util.MimeTypes;

import org.robotv.player.Player;
import org.robotv.player.StreamBundle;
import org.xvdr.recordings.activity.PlayerActivity;
import org.robotv.client.model.Movie;
import org.xvdr.recordings.presenter.ActionPresenterSelector;
import org.xvdr.recordings.presenter.ColorAction;
import org.xvdr.recordings.presenter.DetailsDescriptionPresenter;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

import java.util.Locale;

public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackFragment {

    private Player player;
    private long selectedTrackId = 0;

    public void setPlayer(Player player) {
        this.player = player;
    }

    private SimpleTarget<Bitmap> controlsRowTarget = new SimpleTarget<Bitmap>() {
        @Override
        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
            mPlaybackControlsRow.setImageBitmap(PlaybackOverlayFragment.this.getActivity(), resource);
            mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
        }
    };

    private static final String TAG = PlaybackOverlayFragment.class.getSimpleName();
    private static final int DEFAULT_UPDATE_PERIOD = 1000;

    private Movie mSelectedMovie;
    private int mCurrentPlaybackState;
    private Handler mHandler;
    private Runnable mRunnable;

    private PlaybackControlsRow mPlaybackControlsRow;
    private ArrayObjectAdapter mPrimaryActionAdapter;
    private ArrayObjectAdapter audioTrackActionAdapter;

    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSelectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);
        mHandler = new Handler(Looper.getMainLooper());

        setBackgroundType(PlaybackOverlayFragment.BG_LIGHT);
        setControlsOverlayAutoHideEnabled(false);

        setUpRows();
    }

    private ArrayObjectAdapter mRowsAdapter;

    private void setUpRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        playbackControlsRowPresenter = new PlaybackControlsRowPresenter(new DetailsDescriptionPresenter());

        playbackControlsRowPresenter.setBackgroundColor(Utils.getColor(getActivity(), R.color.primary_color));

        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        addPlaybackControlsRow();

        /* onClick */
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if(action.getId() == mPlayPauseAction.getId()) {
                    togglePlayback(mPlayPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
                }
                else if(action.getId() == mFastForwardAction.getId()) {
                    fastForward(30 * 1000);
                }
                else if(action.getId() == mRewindAction.getId()) {
                    rewind(30 * 1000);
                }
                else if(action.getId() == mSkipNextAction.getId()) {
                    fastForward(5 * 60 * 1000);
                }
                else if(action.getId() == mSkipPreviousAction.getId()) {
                    rewind(5 * 60 * 1000);
                }

                // audio track selection
                if(action instanceof ColorAction) {
                    String trackId = Long.toString(action.getId());
                    player.selectAudioTrack(trackId);
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

        Glide.with(getActivity())
        .load(url).asBitmap()
        .override(Utils.dpToPx(R.integer.artwork_poster_width, getActivity()),
                Utils.dpToPx(R.integer.artwork_poster_height, getActivity()))
        .into(controlsRowTarget);
    }

    private void addPlaybackControlsRow() {
        mPlaybackControlsRow = new PlaybackControlsRow(mSelectedMovie);

        mPlaybackControlsRow.setCurrentPosition(0);
        mPlaybackControlsRow.setBufferedPosition(0);

        mRowsAdapter.add(mPlaybackControlsRow);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();

        mPrimaryActionAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionAdapter);

        ActionPresenterSelector actionPresenterSelector = new ActionPresenterSelector();
        audioTrackActionAdapter = new ArrayObjectAdapter(actionPresenterSelector);
        mRowsAdapter.add(new ListRow(new HeaderItem(getString(R.string.audiotrack)), audioTrackActionAdapter));

        setOnItemViewClickedListener(new BaseOnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
                Action action = (Action) item;
                player.selectAudioTrack(Long.toString(action.getId()));
            }
        });

        Activity activity = getActivity();

        // primary actions
        mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(activity);
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(activity);
        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(activity);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(activity);
        mRewindAction = new PlaybackControlsRow.RewindAction(activity);

        // PrimaryAction setting
        mPrimaryActionAdapter.add(mSkipPreviousAction);
        mPrimaryActionAdapter.add(mRewindAction);
        mPrimaryActionAdapter.add(mPlayPauseAction);
        mPrimaryActionAdapter.add(mFastForwardAction);
        mPrimaryActionAdapter.add(mSkipNextAction);

        updateVideoImage(mSelectedMovie.getPosterUrl());
    }

    private void setAudioTrackActionColor(ColorAction action, long id) {
        action.setColor(
            (id == action.getId()) ? Utils.getColor(getActivity(), R.color.primary_color) :
            Utils.getColor(getActivity(), R.color.default_background)
        );
    }

    public void updateAudioTrackSelection(long id) {
        selectedTrackId = id;

        for(int i = 0; i < audioTrackActionAdapter.size(); i++) {
            ColorAction action = (ColorAction) audioTrackActionAdapter.get(i);
            setAudioTrackActionColor(action, selectedTrackId);
        }

        audioTrackActionAdapter.notifyArrayItemRangeChanged(0, audioTrackActionAdapter.size());
    }

    public void updateAudioTracks(StreamBundle bundle) {
        Log.d(TAG, "updateAudioTracks");
        audioTrackActionAdapter.clear();

        int trackCount = bundle.getStreamCount(StreamBundle.CONTENT_AUDIO);

        for(int i = 0; i < trackCount; i++) {
            StreamBundle.Stream stream = bundle.getStream(StreamBundle.CONTENT_AUDIO, i);
            Log.d(TAG, "pid: " + stream.physicalId);

            String audioType =
                (stream.channels == 6) ? "5.1" :
                (stream.channels == 5) ? "5.0" :
                (stream.channels == 2) ? "Stereo" :
                "";

            int audioFormatIcon =
                (stream.getMimeType().equals(MimeTypes.AUDIO_AC3)) ? R.drawable.ic_launcher_dd :
                (stream.getMimeType().equals(MimeTypes.AUDIO_MPEG)) ? R.drawable.ic_launcher_stereo :
                R.drawable.ic_audiotrack_white_48dp;

            // translate track language
            Locale l = new Locale.Builder().setLanguage(stream.language).build();

            ColorAction action = new ColorAction(
                stream.physicalId,
                audioType,
                l.getDisplayLanguage(),
                getResources().getDrawable(audioFormatIcon, null)
            );

            setAudioTrackActionColor(action, selectedTrackId);

            audioTrackActionAdapter.add(action);
        }

        audioTrackActionAdapter.notifyArrayItemRangeChanged(0, trackCount);
    }

    public void startProgressAutomation() {
        if(mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    long durationMs = player.getDuration();
                    if(durationMs > 0) {
                        mPlaybackControlsRow.setDuration((int) durationMs);
                    }

                    long timeMs = player.getDurationSinceStart();
                    if(timeMs <= player.getDuration()) {
                        mPlaybackControlsRow.setCurrentPosition((int) timeMs);
                    }

                    mRowsAdapter.notifyArrayItemRangeChanged(0, 1);

                    ((PlayerActivity)getActivity()).updatePlaybackState();

                    mHandler.postDelayed(this, DEFAULT_UPDATE_PERIOD);
                }
            };
            mHandler.post(mRunnable);
        }
    }

    public void stopProgressAutomation() {
        if(mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mRunnable = null;
        }
    }

    private void play() {
        mCurrentPlaybackState = PlaybackState.STATE_PLAYING;
        setControlsOverlayAutoHideEnabled(true);
        mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
        mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE));
        notifyChanged(mPlayPauseAction);
    }

    private void pause() {
        mCurrentPlaybackState = PlaybackState.STATE_PAUSED;
        stopProgressAutomation();
        setControlsOverlayAutoHideEnabled(false); // if set to false, PlaybackcontrolsRow will always be on the screen
        mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
        mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.INDEX_PLAY));
        notifyChanged(mPlayPauseAction);
    }

    public void playbackStateChanged() {
        mPlaybackControlsRow.setDuration((int) player.getDuration());

        if(mCurrentPlaybackState != PlaybackState.STATE_PLAYING) {
            play();
        }
        else {
            pause();
        }

        ((PlayerActivity)getActivity()).updatePlaybackState();
    }

    private void notifyChanged(Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionAdapter;

        if(adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }

        adapter = audioTrackActionAdapter;

        if(adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
        }
    }

    public void fastForward(int timeMs) {
        stopProgressAutomation();
        long position = player.getCurrentPosition() + timeMs;
        player.seek(position);
    }

    public void rewind(int timeMs) {
        stopProgressAutomation();
        long position = Math.max(player.getCurrentPosition() - timeMs, player.getStartPosition());
        player.seek(position);
    }

    public void togglePlayback(boolean play) {
        if(play) {
            player.play();
        }
        else {
            player.pause();
        }

        playbackStateChanged();
    }

    public boolean isPlaying() {
        return (mCurrentPlaybackState != PlaybackState.STATE_PLAYING);
    }
}
