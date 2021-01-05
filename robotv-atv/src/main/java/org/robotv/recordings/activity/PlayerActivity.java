package org.robotv.recordings.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;

import com.google.android.exoplayer2.Format;

import org.robotv.client.MovieController;
import org.robotv.player.Player;
import org.robotv.player.StreamBundle;
import org.robotv.recordings.fragment.PlaybackOverlayFragment;
import org.robotv.recordings.fragment.VideoDetailsFragment;
import org.robotv.client.model.Movie;
import org.robotv.recordings.homescreen.RoboTVChannel;
import org.robotv.robotv.R;
import org.robotv.dataservice.DataService;
import org.robotv.dataservice.NotificationHandler;
import org.robotv.setup.SetupUtils;
import org.robotv.ui.DataServiceActivity;

public class PlayerActivity extends DataServiceActivity implements Player.Listener, DataService.Listener {

    public static final String TAG = "PlayerActivity";

    // recording margin at start
    private static final long MARGIN_START = 2 * 60 * 1000;

    // recording margin at start
    private static final long MARGIN_END = 10 * 60 * 1000;

    private Player mPlayer;
    private PlaybackOverlayFragment mControls;
    private Movie mSelectedMovie;
    private NotificationHandler notificationHandler;
    private long lastUpdateTimeStamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        notificationHandler = new NotificationHandler(this);
        lastUpdateTimeStamp = 0;

        mControls = (PlaybackOverlayFragment) getSupportFragmentManager().findFragmentById(R.id.playback);

        mPlayer = new Player(
            this,
            SetupUtils.getServer(this),                       // Server
            SetupUtils.getLanguage(this),                     // Language
            this,                                      // Listener
            SetupUtils.getPassthrough(this),                  // AC3 passthrough
            SetupUtils.getTunneledVideoPlaybackEnabled(this)
        );

        mControls.setPlayer(mPlayer);
        setServiceListener(this);
    }

   private void startPlayback() {
        if(mSelectedMovie == null) {
            return;
        }

        String recid = mSelectedMovie.getRecordingIdString();

        DataService service = getService();
        MovieController controller = service.getMovieController();
        long position = controller.getPlaybackPosition(recid);

        mPlayer.open(Player.createRecordingUri(recid, position));
        mControls.togglePlayback(true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        updatePlaybackPosition(true);
        boolean finished = shouldRemoveWatchNext();

        stopPlayback();

        if(mPlayer != null) {
            mPlayer.release();
        }

        if(finished) {
            setPlaybackPosition(0);
        }

        super.onDestroy();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if(playWhenReady && (playbackState == com.google.android.exoplayer2.Player.STATE_READY)) {
            mControls.startProgressAutomation();
        }
    }

    private boolean shouldUpdateWatchNext() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }

        long lastPosition = mPlayer.getDurationSinceStart(); // duration since start in ms
        long duration = mPlayer.getDuration();

        long durationWatched = Math.max(0, lastPosition - MARGIN_START);
        long durationWithoutMargin = Math.max(0, duration - (MARGIN_END + MARGIN_START));

        if(durationWithoutMargin == 0) {
            return false;
        }

        long durationPercentage = (100 * durationWatched) / durationWithoutMargin;

        return durationWatched > 2 * 60 * 1000 || durationPercentage > 3;
    }

    private boolean shouldRemoveWatchNext() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        long lastPosition = mPlayer.getDurationSinceStart(); // duration since start in ms
        long duration = mPlayer.getDuration();

        long durationWatched = Math.max(0, lastPosition - MARGIN_START);
        long durationWithoutMargin = Math.max(0, duration - (MARGIN_END + MARGIN_START));
        long durationLeft = Math.max(0, durationWithoutMargin - durationWatched);

        return durationLeft < 8;
    }

    public void updatePlaybackPosition(boolean force) {
        long now = System.currentTimeMillis();
        long lastPosition = mPlayer.getDurationSinceStart(); // duration since start in ms
        long duration = mPlayer.getDuration();

        if((now - lastUpdateTimeStamp < 5000 || lastPosition == 0) && !force) {
            return;
        }

        if(shouldUpdateWatchNext()) {
            RoboTVChannel.addWatchNext(this, mSelectedMovie, lastPosition, duration);
        }

        if(shouldRemoveWatchNext()) {
            RoboTVChannel.removeWatchNext(this, mSelectedMovie);
        }

        setPlaybackPosition(lastPosition);
    }

    private void setPlaybackPosition(long position) {
        DataService service = getService();

        if(service != null) {
            service.getMovieController().setPlaybackPosition(mSelectedMovie, position);
        }

        lastUpdateTimeStamp = System.currentTimeMillis();
    }

    protected void stopPlayback() {
        if(mPlayer == null) {
            return;
        }

        mControls.stopProgressAutomation();

        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;

        finishAndRemoveTask();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mPlayer == null || keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            return super.onKeyDown(keyCode, event);
        }

        mControls.togglePlayback(mControls.isPlaying());
        return true;
    }

    @Override
    public void onPlayerError(Exception e) {
    }

    @Override
    public void onDisconnect() {
        mPlayer.pause();
    }

    @Override
    public void onTracksChanged(final StreamBundle bundle) {
        runOnUiThread(() -> mControls.updateAudioTracks(bundle));
    }

    @Override
    public void onAudioTrackChanged(final Format format) {
        if(format == null) {
            return;
        }

        runOnUiThread(() -> mControls.updateAudioTrackSelection(Long.parseLong(format.id)));
    }

    @Override
    public void onVideoTrackChanged(Format format) {
    }

    @Override
    public void onRenderedFirstFrame() {
    }

    @Override
    public void onStreamError(int status) {
        notificationHandler.error(getString(R.string.error_open_recording));
    }

    @Override
    public void onConnected(DataService service) {
        // check if thats a service reconnect
        if(mPlayer != null && mPlayer.getPlaybackState() > com.google.android.exoplayer2.Player.STATE_IDLE) {
            return;
        }

        String recid = null;

        Intent intent = getIntent();
        String action = intent.getAction();

        if(action != null && action.equals("android.intent.action.VIEW")) {
            recid = intent.getDataString();
        }

        if(TextUtils.isEmpty(recid)) {
            recid = (String) getIntent().getSerializableExtra(VideoDetailsFragment.EXTRA_RECID);
        }

        Log.d(TAG, "recid: " + recid);
        MovieController controller = service.getMovieController();

        if(controller != null) {
            mSelectedMovie = controller.getMovie(recid);
        }

        if(mSelectedMovie == null) {
            notificationHandler.error(getString(R.string.failed_to_fetch_movie_information));
            finish();
            return;
        }

        SurfaceView mVideoView = findViewById(R.id.videoView);
        mPlayer.setSurface(mVideoView.getHolder().getSurface());

        mControls.setMovie(mSelectedMovie);
        mControls.setUpRows();

        startPlayback();
    }

    @Override
    public void onConnectionError(DataService service) {

    }

    @Override
    public void onMovieUpdate(DataService service) {
    }

    @Override
    public void onTimersUpdated(DataService service) {
    }
}
