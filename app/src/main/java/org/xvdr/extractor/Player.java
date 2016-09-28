package org.xvdr.extractor;

import android.content.Context;
import android.media.MediaCodec;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;


import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.robotv.client.StreamBundle;

import java.io.IOException;

public class Player implements ExoPlayer.EventListener, VideoRendererEventListener, RoboTvExtractor.Listener, RoboTvDataSourceFactory.Listener, MappingTrackSelector.EventListener {

    private static final String TAG = "Player";

    public final static int CHANNELS_DEFAULT = 0;
    public final static int CHANNELS_STEREO = 2;
    public final static int CHANNELS_SURROUND = 4;
    public final static int CHANNELS_DIGITAL51 = 6;

    private final static int WIND_UPDATE_PERIOD_MS = 1000;

    public interface Listener  {

        void onPlayerStateChanged(boolean playWhenReady, int playbackState);

        void onPlayerError(Exception e);

        void onDisconnect();

        void onReconnect();

        void onTracksChanged(StreamBundle bundle);

        void onAudioTrackChanged(Format format);

        void onVideoTrackChanged(Format format);
    }

    private MediaCodecVideoRenderer mVideoRenderer = null;
    private RoboTvAudioRenderer mAudioRenderer = null;

    private Listener mListener;
    private Handler mHandler;
    private Surface mSurface;

    final private ExoPlayer mExoPlayer;
    final private DefaultTrackSelector trackSelector;
    final private RoboTvDataSourceFactory dataSourceFactory;
    final private RoboTvExtractor.Factory extractorFactory;
    final private PositionReference position;

    private Context mContext;

    private boolean mAudioPassthrough;
    private int mChannelConfiguration;

    private Runnable mWindRunnable = null;

    static public Uri createLiveUri(int channelUid) {
        return Uri.parse("robotv://livetv/" + channelUid);
    }

    static public Uri createRecordingUri(String recordingId) {
        return Uri.parse("robotv://recording/" + recordingId);
    }

    public Player(Context context, String server, String language, Listener listener) throws IOException {
        this(context, server, language, listener, false, CHANNELS_SURROUND);
    }

    public Player(Context context, String server, String language, Listener listener, boolean audioPassthrough) throws IOException {
        this(context, server, language, listener, audioPassthrough, CHANNELS_SURROUND);
    }

    public Player(Context context, String server, String language, Listener listener, boolean audioPassthrough, int wantedChannelConfiguration) throws IOException {
        mContext = context;
        mListener = listener;
        mAudioPassthrough = audioPassthrough;
        mChannelConfiguration = CHANNELS_DIGITAL51;

        //mHandlerThread = new HandlerThread("robotv:eventhandler", android.os.Process.THREAD_PRIORITY_DEFAULT);
        //mHandlerThread.start();
        //mHandler = new Handler(mHandlerThread.getLooper());
        mHandler = new Handler();

        position = new PositionReference();

        mVideoRenderer = new MediaCodecVideoRenderer(
                mContext,
                MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT,
                2000, // joining time
                null,
                true,
                mHandler,
                this,
                50);

        mAudioRenderer = new RoboTvAudioRenderer(
                mHandler,
                null);

        Renderer[] renderers = { mVideoRenderer, mAudioRenderer };

        trackSelector = new DefaultTrackSelector(mHandler);
        trackSelector.addListener(this);
        trackSelector.setPreferredLanguages(language);

        DefaultLoadControl loadControl = new DefaultLoadControl(
                new DefaultAllocator(C.DEFAULT_BUFFER_SEGMENT_SIZE),
                2000,
                3000,
                1000,
                2000
        );

        mExoPlayer = ExoPlayerFactory.newInstance(renderers, trackSelector, loadControl);
        mExoPlayer.addListener(this);

        dataSourceFactory = new RoboTvDataSourceFactory(position, language, this);
        dataSourceFactory.connect(server);

        extractorFactory = new RoboTvExtractor.Factory(position, this);
    }

    public void release() {
        stop();

        mHandler = null;

        mExoPlayer.removeListener(this);
        mExoPlayer.release();

        dataSourceFactory.release();

        mVideoRenderer = null;
        mAudioRenderer = null;

        //mHandlerThread.interrupt();
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setStreamVolume(float volume) {
        // TODO - implement stream volume
        /*if(mAudioRenderer != null) {
            mExoPlayer.sendMessage(mAudioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, volume);
        }*/
    }

    public void play() {
        mExoPlayer.setPlayWhenReady(true);
    }

    public void pause(boolean on) {
        mExoPlayer.setPlayWhenReady(!on);
        //setPlaybackParams(1);
    }

    public boolean isPaused() {
        return !mExoPlayer.getPlayWhenReady();
    }

    public void stop() {
        mExoPlayer.stop();
        position.reset();
    }

    public boolean open(Uri uri) {
        stop();

        MediaSource source = new ExtractorMediaSource(
                uri,
                dataSourceFactory,
                extractorFactory,
                null, null
        );

        mExoPlayer.prepare(source);

        ExoPlayer.ExoPlayerMessage[] messages = {
                new ExoPlayer.ExoPlayerMessage(mVideoRenderer, C.MSG_SET_SURFACE,  mSurface)
        };

        if (mSurface == null) {
            mExoPlayer.blockingSendMessages(messages);
        }
        else {
            mExoPlayer.sendMessages(messages);
        }

        return true;
    }

    public boolean selectAudioTrack(int trackId) {
        return true; // TODO - implement selectAudioTrack
    }

    public long getStartPosition() {
        return position.getStartPosition();
    }

    public long getEndPosition() {
        return position.getEndPosition();
    }

    public long getCurrentPosition() {
        return position.getCurrentPosition();
    }

    public long getDuration() {
        return position.getDuration();
    }

    public int getPlaybackState() {
        return mExoPlayer.getPlaybackState();
    }

    public void seek(long position) {
        long p = this.position.timeUsFromPosition(position);

        Log.d(TAG, "seek position   : " + (position / 1000) + " sec");
        mExoPlayer.seekTo(p / 1000);
    }

    private void stopWinding() {
        if(mWindRunnable == null) {
            return;
        }

        mHandler.removeCallbacks(mWindRunnable);
        mWindRunnable = null;
    }

    private void startWinding(final int speed) {
        stopWinding();

        mWindRunnable = new Runnable() {
            @Override
            public void run() {
                long diff = speed * WIND_UPDATE_PERIOD_MS;
                long pos = getCurrentPosition() + diff;

                Log.d(TAG, "position: " + pos);
                mExoPlayer.seekTo(pos / 1000);

                mHandler.postDelayed(mWindRunnable, WIND_UPDATE_PERIOD_MS);
            }
        };

        mHandler.postDelayed(mWindRunnable, WIND_UPDATE_PERIOD_MS);
    }

    public void setPlaybackParams(PlaybackParams params) {
        /*Log.d(TAG, "playback speed: " + speed);

        // just shift timestamps if we're paused
        if(isPaused() && speed != 1) {
            startWinding(speed);
            return;
        }

        if(mWindRunnable != null) {
            stopWinding();
            mExoPlayer.seekTo(getCurrentPosition() / 1000);
        }

        // TODO - reverse playback
        if(speed < 0) {
            return;
        }*/

        // TODO - handle trick play
        /*if(speed == mSampleSource.getPlaybackSpeed()) {
            return;
        }*/

        // remove pending audio
        /*mSampleSource.clearAudioTrack();

        if(speed == 1) {
            mExoPlayer.setSelectedTrack(RoboTvSampleSource.TRACK_AUDIO, ExoPlayer.TRACK_DEFAULT);
        }
        else {
            mExoPlayer.setSelectedTrack(RoboTvSampleSource.TRACK_AUDIO, ExoPlayer.TRACK_DISABLED);
        }

        mSampleSource.setPlaybackParams((int)speed);*/
    }

    static String nameOfChannelConfiguration(int channelConfiguration) {
        switch(channelConfiguration) {
            case CHANNELS_DEFAULT:
                return "default (unknown)";

            case CHANNELS_STEREO:
                return "stereo";

            case CHANNELS_SURROUND:
                return "surround";

            case CHANNELS_DIGITAL51:
                return "digital51";
        }

        return "invalid configuration";
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.i(TAG, "onPlayerStateChanged " + playWhenReady + " " + playbackState);
        mListener.onPlayerStateChanged(playWhenReady, playbackState);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        mListener.onPlayerError(error);
    }

    @Override
    public void onTracksChanged(StreamBundle bundle) {
        mListener.onTracksChanged(bundle);
    }

    @Override
    public void onPositionDiscontinuity() {
    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
    }

    @Override
    public void onDisconnect() {
        mListener.onDisconnect();
    }

    @Override
    public void onReconnect() {
        mListener.onReconnect();
    }

    @Override
    public void onTracksChanged(MappingTrackSelector.TrackInfo trackInfo) {
        if(mListener == null) {
            return;
        }

        for(int i = 0; i < trackInfo.rendererCount; i++) {
            TrackSelection selection = trackInfo.getTrackSelection(i);
            Format format = selection.getSelectedFormat();

            // selected audio track
            if(format.sampleMimeType.startsWith("audio")) {
                mListener.onAudioTrackChanged(format);
            }

            // selected video track
            if(format.sampleMimeType.startsWith("video")) {
                mListener.onVideoTrackChanged(format);
            }
        }

    }

}
