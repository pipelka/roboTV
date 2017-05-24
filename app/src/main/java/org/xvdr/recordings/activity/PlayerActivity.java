package org.xvdr.recordings.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.text.TextUtils;
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
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.NotificationHandler;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.StreamBundle;
import org.xvdr.ui.DataServiceActivity;

import java.io.IOException;

public class PlayerActivity extends DataServiceActivity implements Player.Listener, DataService.Listener {

    public static final String TAG = "PlayerActivity";

    private Player mPlayer;
    private PlaybackOverlayFragment mControls;
    private Movie mSelectedMovie;
    private MediaSession mSession;
    private NotificationHandler notificationHandler;
    private long lastUpdateTimeStamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        notificationHandler = new NotificationHandler(this);
        lastUpdateTimeStamp = 0;

        mControls = (PlaybackOverlayFragment) getFragmentManager().findFragmentById(R.id.playback);

        try {
            mPlayer = new Player(
                this,
                SetupUtils.getServer(this),                     // Server
                SetupUtils.getLanguageISO3(this),               // Language
                this,                                           // Listener
                SetupUtils.getPassthrough(this)                 // AC3 passthrough
            );
        }
        catch(IOException e) {
            e.printStackTrace();
            return;
        }

        mControls.setPlayer(mPlayer);

        mSession = new MediaSession(this, "roboTV Movie");
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        initViews();
        setServiceListener(this);
    }

    private void updateMetadata(Movie movie) {
        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        metadataBuilder
        .putLong(MediaMetadata.METADATA_KEY_DURATION, mSelectedMovie.getDurationMs())
        .putString(MediaMetadata.METADATA_KEY_TITLE, movie.getTitle())
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, movie.getShortText())
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, movie.getDescription());

        String url = movie.getPosterUrl();

        if(!TextUtils.isEmpty(url)) {
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
        updatePlaybackPosition();
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

        DataService service = getService();
        long position = 0;

        if(service != null) {
            position = service.getMovieController().getPlaybackPosition(mSelectedMovie);
        }

        String id = mSelectedMovie.getRecordingIdString();

        mPlayer.open(Player.createRecordingUri(id, position));
        mControls.togglePlayback(true);

        mSession.setActive(true);
    }

    @Override
    public void onPause() {
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

        stopPlayback();

        if(mPlayer != null) {
            mPlayer.release();
        }

        Glide.get(this).clearMemory();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    }

    void updatePlaybackPosition() {
        long now = System.currentTimeMillis();

        if(now - lastUpdateTimeStamp < 5000) {
            return;
        }

        DataService service = getService();
        long lastPosition = mPlayer.getDurationSinceStart(); // duration since start in ms

        if(service != null) {
            service.getMovieController().setPlaybackPosition(mSelectedMovie, lastPosition);
        }

        lastUpdateTimeStamp = now;
    }

    protected void stopPlayback() {
        mSession.setActive(false);
        mSession.release();

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
    public void onPlayerError(Exception e) {
    }

    @Override
    public void onDisconnect() {
        mPlayer.pause();
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
    public void onRenderedFirstFrame() {
        mControls.startProgressAutomation();
    }

    @Override
    public void onStreamError(int status) {
        notificationHandler.error(getString(R.string.error_open_recording));
    }

    @Override
    public void onConnected(DataService service) {
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
