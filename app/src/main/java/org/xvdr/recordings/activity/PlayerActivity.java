package org.xvdr.recordings.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;

import org.xvdr.extractor.Player;
import org.xvdr.extractor.RecordingPlayer;
import org.xvdr.msgexchange.Packet;
import org.xvdr.recordings.fragment.PlaybackOverlayFragment;
import org.xvdr.recordings.fragment.VideoDetailsFragment;
import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.StreamBundle;

public class PlayerActivity extends Activity implements Player.Listener {

    public static final String TAG = "PlayerActivity";
    public static final String EXTRA_START_POSITION = "extra_start_position";

    private RecordingPlayer mPlayer;
    private PlaybackOverlayFragment mControls;
    private SurfaceView mVideoView;
    private Movie mSelectedMovie;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mControls = (PlaybackOverlayFragment) getFragmentManager().findFragmentById(R.id.playback);
        mPlayer = new RecordingPlayer(this, SetupUtils.getServer(this), SetupUtils.getLanguageISO3(this), this);
        initViews();
        startVideoPlayer();
    }

    private void initViews() {
        mVideoView = (SurfaceView) findViewById(R.id.videoView);
        mPlayer.setSurface(mVideoView.getHolder().getSurface());
        mSelectedMovie = (Movie) getIntent().getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);
    }

    private void startVideoPlayer() {
        Bundle bundle = getIntent().getExtras();

        if(mSelectedMovie == null || bundle == null) {
            return;
        }

        //mPosition = bundle.getInt(EXTRA_START_POSITION, 0);

        mPlayer.openRecording(mSelectedMovie.getId());
        mControls.togglePlayback(true);
    }

    public int getCurrentTime() {
        return (int)(mPlayer.getCurrentPositionWallclock() - mPlayer.getStartPositionWallclock());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.stop();
        mPlayer.release();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    }

    public void playPause(boolean doPlay) {
        mPlayer.pause(!doPlay);
    }

    public void fastForward(int timeMs) {
        mPlayer.seekTo(mPlayer.getCurrentPositionWallclock() + timeMs);
    }

    public void rewind(int timeMs) {
        mPlayer.seekTo(mPlayer.getCurrentPositionWallclock() - timeMs);
    }

    @Override
    public void onPlayerError(Exception e) {
    }

    @Override
    public void onNotification(Packet notification) {
    }

    @Override
    public void onDisconnect() {
        playPause(false);
    }

    @Override
    public void onReconnect() {
        playPause(true);
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
