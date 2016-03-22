package org.xvdr.recordings.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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

    private RecordingPlayer mPlayer;
    private PlaybackOverlayFragment mControls;
    private Movie mSelectedMovie;
    private MediaSession mSession;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mControls = (PlaybackOverlayFragment) getFragmentManager().findFragmentById(R.id.playback);
        mPlayer = new RecordingPlayer(
            this,
            SetupUtils.getServer(this),
            SetupUtils.getLanguageISO3(this),
            this,
            SetupUtils.getPassthrough(this),
            SetupUtils.getSpeakerConfiguration(this)
        );

        mSession = new MediaSession(this, "roboTV Movie");
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        initViews();
        startPlayback();
    }

    private void updateMetadata(Movie movie) {
        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        metadataBuilder
        .putLong(MediaMetadata.METADATA_KEY_DURATION, mSelectedMovie.getDurationMs() * 1000)
        .putString(MediaMetadata.METADATA_KEY_TITLE, movie.getTitle())
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, movie.getOutline())
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, movie.getDescription());

        String url = movie.getCardImageUrl();

        if(url != null && !url.isEmpty()) {
            Picasso
            .with(this)
            .load(url)
            .resize(266, 400)
            .centerCrop()
            .into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                    MediaMetadata m = metadataBuilder.build();
                    mSession.setMetadata(m);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            });
            return;
        }

        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.drawable.recording_unkown));
        mSession.setMetadata(metadataBuilder.build());
    }

    public void updatePlaybackState() {
        long position = getCurrentTime();

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder();
        stateBuilder.setState(mControls.getPlaybackState(), position, 1.0f);

        mSession.setPlaybackState(stateBuilder.build());
    }

    private void initViews() {
        SurfaceView mVideoView = (SurfaceView) findViewById(R.id.videoView);
        mPlayer.setSurface(mVideoView.getHolder().getSurface());
        mSelectedMovie = (Movie) getIntent().getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);

        updateMetadata(mSelectedMovie);
    }

    private void startPlayback() {
        Bundle bundle = getIntent().getExtras();

        if(mSelectedMovie == null || bundle == null) {
            return;
        }

        String id = mSelectedMovie.getId();
        mPlayer.openRecording(id, true);
        mControls.togglePlayback(true);

        mSession.setActive(true);
    }

    public int getCurrentTime() {
        return Math.max((int)(mPlayer.getCurrentPositionWallclock() - mPlayer.getStartPositionWallclock()), 0);
    }

    public int getTotalTime() {
        if(mSelectedMovie == null) {
            return 0;
        }

        if(mPlayer == null) {
            return (int)mSelectedMovie.getDurationMs();
        }

        if(mPlayer.getEndPositionWallclock() == -1) {
            return mPlayer.getDurationMs();
        }

        return (int)(mPlayer.getEndPositionWallclock() - mPlayer.getStartPositionWallclock());
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(!requestVisibleBehind(true)) {
            stopPlayback();
        }
    }

    @Override
    public void onVisibleBehindCanceled() {
        // App-specific method to stop playback and release resources
        super.onVisibleBehindCanceled();
        stopPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    }

    protected void stopPlayback() {
        mSession.setActive(false);
        mSession.release();

        if(mPlayer == null) {
            return;
        }

        mControls.stopProgressAutomation();

        mPlayer.setLastPosition(getCurrentTime());
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;

        finishAndRemoveTask();
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

    public void restart() {
        mPlayer.seekTo(mPlayer.getStartPositionWallclock());
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
