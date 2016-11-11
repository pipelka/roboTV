package org.xvdr.recordings.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;

import org.xvdr.player.Player;
import org.xvdr.recordings.fragment.PlaybackOverlayFragment;
import org.xvdr.recordings.fragment.VideoDetailsFragment;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.NotificationHandler;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.StreamBundle;

import java.io.IOException;

public class PlayerActivity extends Activity implements Player.Listener {

    public static final String TAG = "PlayerActivity";

    private Player mPlayer;
    private PlaybackOverlayFragment mControls;
    private Movie mSelectedMovie;
    private MediaSession mSession;
    private NotificationHandler notificationHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        notificationHandler = new NotificationHandler(this);

        mControls = (PlaybackOverlayFragment) getFragmentManager().findFragmentById(R.id.playback);

        try {
            mPlayer = new Player(
                this,
                SetupUtils.getServer(this),                     // Server
                SetupUtils.getLanguageISO3(this),               // Language
                this,                                           // Listener
                SetupUtils.getPassthrough(this),                // AC3 passthrough
                SetupUtils.getSpeakerConfiguration(this));      // preferred channel configuration
        }
        catch(IOException e) {
            e.printStackTrace();
            return;
        }

        mControls.setPlayer(mPlayer);

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
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, movie.getShortText())
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, movie.getDescription());

        String url = movie.getPosterUrl();

        if(url != null && !url.isEmpty()) {
            Glide.with(this).load(url).asBitmap()
            .override(Utils.dpToPx(R.integer.artwork_poster_width, this), Utils.dpToPx(R.integer.artwork_poster_height, this))
            .centerCrop().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, resource);
                    MediaMetadata m = metadataBuilder.build();
                    mSession.setMetadata(m);
                }
            });
            return;
        }

        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.drawable.recording_unkown));
        mSession.setMetadata(metadataBuilder.build());
    }

    public void updatePlaybackState() {
        long position = mPlayer.getDurationSinceStart();

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder();

        int playerState = mPlayer.getPlaybackState();
        int state = PlaybackState.STATE_NONE;

        if(playerState == ExoPlayer.STATE_BUFFERING) {
            state = PlaybackState.STATE_BUFFERING;
        }
        else if(mPlayer.isPaused()) {
            state = PlaybackState.STATE_PAUSED;
        }
        else if(!mPlayer.isPaused()) {
            state = PlaybackState.STATE_PLAYING;
        }

        stateBuilder.setState(state, position, 1.0f);

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

        String id = mSelectedMovie.getRecordingIdString();

        mPlayer.open(Player.createRecordingUri(id));
        mControls.togglePlayback(true);

        mSession.setActive(true);
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

        if(mPlayer != null) {
            mPlayer.release();
        }

        Glide.get(this).clearMemory();
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

        //mPlayer.setLastPosition(getCurrentTime()); - TODO
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;

        finishAndRemoveTask();
    }

    @Override
    public void onPlayerError(Exception e) {
    }

    @Override
    public void onDisconnect() {
        mPlayer.pause();
    }

    @Override
    public void onReconnect() {
        mPlayer.play();
    }

    @Override
    public void onTracksChanged(final StreamBundle bundle) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControls.updateAudioTracks(bundle);
            }
        });
    }

    @Override
    public void onAudioTrackChanged(final Format format) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControls.updateAudioTrackSelection(Long.parseLong(format.id));
            }
        });
    }

    @Override
    public void onVideoTrackChanged(Format format) {
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
    }

    @Override
    public void onStreamError(int status) {
        notificationHandler.error(getString(R.string.error_open_recording));
    }

}
