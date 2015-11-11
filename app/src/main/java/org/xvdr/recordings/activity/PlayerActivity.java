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
import android.widget.ProgressBar;
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

    private final double MEDIA_BAR_TOP_MARGIN = 0.8;
    private final double MEDIA_BAR_RIGHT_MARGIN = 0.2;
    private final double MEDIA_BAR_BOTTOM_MARGIN = 0.0;
    private final double MEDIA_BAR_LEFT_MARGIN = 0.2;
    private final double MEDIA_BAR_HEIGHT = 0.1;
    private final double MEDIA_BAR_WIDTH = 0.9;

    private RecordingPlayer mPlayer;
    private SurfaceView mVideoView;
    private TextView mStartText;
    private TextView mEndText;
    private SeekBar mSeekbar;
    private ImageView mPlayPause;
    private View mControllers;
    private Timer mSeekbarTimer;
    private Timer mControllersTimer;
    private int mPlaybackState;
    private final Handler mHandler = new Handler();
    private Movie mSelectedMovie;
    private int mDuration;
    private DisplayMetrics mMetrics;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mPlayer = new RecordingPlayer(this, SetupUtils.getServer(this), SetupUtils.getLanguageISO3(this), this);
        initWindow();
        initViews();
        setupController();
        startVideoPlayer();
        mVideoView.invalidate();
    }

    private void initWindow() {
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void initViews() {
        mVideoView = (SurfaceView) findViewById(R.id.videoView);
        mStartText = (TextView) findViewById(R.id.startText);
        mEndText = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar);
        mSeekbar.setFocusable(false);

        mPlayPause = (ImageView) findViewById(R.id.playpause);
        mControllers = findViewById(R.id.controllers);

        mPlayer.setSurface(mVideoView.getHolder().getSurface());

        mVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mControllers.getVisibility() != View.VISIBLE) {
                    mControllers.setVisibility(View.VISIBLE);
                }

                if (mPlaybackState == ExoPlayer.STATE_IDLE) {
                    mPlayer.play();
                    startControllersTimer();
                } else {
                    mPlayer.pause();
                    stopControllersTimer();
                }
            }
        });
    }

    private void setupController() {

        int w = (int) (mMetrics.widthPixels * MEDIA_BAR_WIDTH);
        int h = (int) (mMetrics.heightPixels * MEDIA_BAR_HEIGHT);
        int marginLeft = (int) (mMetrics.widthPixels * MEDIA_BAR_LEFT_MARGIN);
        int marginTop = (int) (mMetrics.heightPixels * MEDIA_BAR_TOP_MARGIN);
        int marginRight = (int) (mMetrics.widthPixels * MEDIA_BAR_RIGHT_MARGIN);
        int marginBottom = (int) (mMetrics.heightPixels * MEDIA_BAR_BOTTOM_MARGIN);
        LayoutParams lp = new LayoutParams(w, h);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        mControllers.setLayoutParams(lp);
        mStartText.setText(getResources().getString(R.string.init_text));
        mEndText.setText(getResources().getString(R.string.init_text));
    }

    private void startVideoPlayer() {
        Bundle bundle = getIntent().getExtras();
        mSelectedMovie = (Movie) getIntent().getSerializableExtra( VideoDetailsFragment.EXTRA_MOVIE );

        if( mSelectedMovie == null || bundle == null )
            return;
        
        int startPosition = bundle.getInt(EXTRA_START_POSITION, 0);

        mPlayer.openRecording(mSelectedMovie.getId());
        mDuration = mPlayer.getDurationMs();

        play(startPosition);

        //mPlayPause.requestFocus();
        startControllersTimer();
    }

    private void updatePlaybackLocation() {
        if ( mPlaybackState == ExoPlayer.STATE_BUFFERING ||
            mPlaybackState == ExoPlayer.STATE_READY) {
            startControllersTimer();
        }
        else {
            stopControllersTimer();
        }
    }

    private void play(int position) {
        startControllersTimer();

        if(position > 0) {
            //mPlayer.seekTo(position);
        }

        mPlayer.play();
        restartSeekBarTimer();
    }

    private void stopSeekBarTimer() {
        if( null != mSeekbarTimer ) {
            mSeekbarTimer.cancel();
        }
    }

    private void restartSeekBarTimer() {
        stopSeekBarTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(),
                getResources().getInteger( R.integer.seekbar_delay_time ),
                getResources().getInteger( R.integer.seekbar_interval_time ) );
    }

    private void stopControllersTimer() {
        if ( mControllersTimer != null ) {
            mControllersTimer.cancel();
        }
    }

    private void startControllersTimer() {
        stopControllersTimer();
        mControllersTimer = new Timer();
        mControllersTimer.schedule( new HideControllersTask(), getResources().getInteger( R.integer.time_to_hide_controller ) );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ( mSeekbarTimer != null ) {
            mSeekbarTimer.cancel();
            mSeekbarTimer = null;
        }
        if ( mControllersTimer != null ) {
            mControllersTimer.cancel();
        }
        mPlayer.pause();
        updatePlayButton( mPlaybackState );
    }

    @Override
    protected void onDestroy() {
        stopControllersTimer();
        stopSeekBarTimer();
        super.onDestroy();
        mPlayer.release();
    }

    private class HideControllersTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mControllers.setVisibility( View.GONE );
                }
            });
        }
    }

    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    updateSeekbar((int)mPlayer.getCurrentPosition(), mDuration );
                }
            });
        }
    }

    private class BackToDetailTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post( new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent( getApplicationContext(), DetailsActivity.class );
                    intent.putExtra( VideoDetailsFragment.EXTRA_MOVIE, mSelectedMovie );
                    startActivity( intent );
                    finish();
                }
            });

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown");
        long currentPos = 0;
        int delta = 30000; // 30 seconds

        if ( mControllers.getVisibility() != View.VISIBLE ) {
            mControllers.setVisibility( View.VISIBLE );
        }
        switch ( keyCode ) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                currentPos = mPlayer.getCurrentPosition();
                currentPos -= delta;
                mPlayer.seekTo(currentPos);
                Log.d(TAG, "left - done");
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                currentPos = mPlayer.getCurrentPosition();
                currentPos += delta;
                mPlayer.seekTo(currentPos);
                Log.d(TAG, "right - done");
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void updateSeekbar(int position, int duration) {
        mSeekbar.setProgress( position );
        mSeekbar.setMax( duration );
        mStartText.setText( Utils.formatMillis( position ) );
        mEndText.setText( Utils.formatMillis( duration ) );
    }

    private void updatePlayButton(int state ) {
        switch ( state ) {
            case ExoPlayer.STATE_READY:
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable( R.drawable.ic_pause_playcontrol_normal ) );
                break;
            case ExoPlayer.STATE_IDLE:
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable( R.drawable.ic_play_playcontrol_normal ) );
                break;
            case ExoPlayer.STATE_BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        mPlaybackState = playbackState;
        updatePlayButton(mPlaybackState);
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
