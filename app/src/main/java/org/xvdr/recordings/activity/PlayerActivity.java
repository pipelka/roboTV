package org.xvdr.recordings.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer.ExoPlayer;

import org.xvdr.extractor.Player;
import org.xvdr.extractor.RecordingPlayer;
import org.xvdr.msgexchange.Packet;
import org.xvdr.recordings.fragment.VideoDetailsFragment;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.tv.StreamBundle;

import java.util.Timer;
import java.util.TimerTask;

public class PlayerActivity extends Activity implements Player.Listener {

    public static final String TAG = "PlayerActivity";
    public static final String EXTRA_START_POSITION = "extra_start_position";

    private RecordingPlayer mPlayer;
    private SurfaceView mVideoView;
    private int mPlaybackState;
    private Movie mSelectedMovie;
    private int mDuration = -1;
    private int mPosition = 0;
    private long mStartTimeMillis;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mPlayer = new RecordingPlayer(this, SetupUtils.getServer(this), SetupUtils.getLanguageISO3(this), this);
        initViews();
        startVideoPlayer();
    }

    private void initViews() {
        mVideoView = (SurfaceView) findViewById(R.id.videoView);
        mPlayer.setSurface(mVideoView.getHolder().getSurface());
        mSelectedMovie = (Movie) getIntent().getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);
    }

    private void setPosition(int position) {
        if (position > mDuration) {
            mPosition = (int) mDuration;
        } else if (position < 0) {
            mPosition = 0;
            mStartTimeMillis = System.currentTimeMillis();
        } else {
            mPosition = position;
        }
        mStartTimeMillis = System.currentTimeMillis();
        Log.d(TAG, "position set to " + mPosition);
    }

    public int getPosition() {
        return mPosition;
    }

    private void startVideoPlayer() {
        Bundle bundle = getIntent().getExtras();

        if( mSelectedMovie == null || bundle == null )
            return;

        mPosition = bundle.getInt(EXTRA_START_POSITION, 0);

        mPlayer.openRecording(mSelectedMovie.getId());
        mDuration = (int)mSelectedMovie.getDuration() * 1000;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.stop();
        mPlayer.release();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        mPlaybackState = playbackState;
    }

    public void playPause(boolean doPlay) {
        if (doPlay) {
            mPlayer.play();
            mStartTimeMillis = System.currentTimeMillis();
        }
        else {

            int timeElapsedSinceStart = (int) (System.currentTimeMillis() - mStartTimeMillis);
            setPosition(mPosition + timeElapsedSinceStart);
            mPlayer.pause();
        }
    }

    public void fastForward() {
        if (mDuration != -1) {
            // Fast forward 10 seconds.
            setPosition((int)mPlayer.getCurrentPosition() + (10 * 1000));
            mPlayer.seekTo(mPosition);
        }
    }

    public void rewind() {
        // rewind 10 seconds
        setPosition((int)mPlayer.getCurrentPosition() - (10 * 1000));
        mPlayer.seekTo(mPosition);
    }

    @Override
    public void onPlayerError(Exception e) {
    }

    @Override
    public void onNotification(Packet notification) {
    }

    @Override
    public void onDisconnect() {
    }

    @Override
    public void onReconnect() {
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
    }

    @Override
    public void onTracksChanged(StreamBundle bundle) {
    }

    @Override
    public void onAudioTrackChanged(StreamBundle.Stream stream) {
    }

    @Override
    public void onVideoTrackChanged(StreamBundle.Stream stream) {
    }

    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
    }

}
