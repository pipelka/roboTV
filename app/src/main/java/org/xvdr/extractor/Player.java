package org.xvdr.extractor;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.robotv.client.StreamBundle;

import java.io.IOException;

public class Player implements ExoPlayer.EventListener, VideoRendererEventListener, RoboTvExtractor.Listener, RoboTvDataSourceFactory.Listener, RoboTvTrackSelector.EventListener {

    private static final String TAG = "Player";

    final static int CHANNELS_DEFAULT = 0;
    final static int CHANNELS_STEREO = 2;
    final static int CHANNELS_SURROUND = 4;
    final static int CHANNELS_DIGITAL51 = 6;

    public interface Listener  {

        void onPlayerStateChanged(boolean playWhenReady, int playbackState);

        void onPlayerError(Exception e);

        void onDisconnect();

        void onReconnect();

        void onTracksChanged(StreamBundle bundle);

        void onAudioTrackChanged(Format format);

        void onVideoTrackChanged(Format format);

        void onRenderedFirstFrame(Surface surface);

        void onStreamError(int status);
    }

    private Renderer mVideoRenderer = null;
    private Renderer mInternalAudioRenderer = null;
    private Renderer mExoAudioRenderer = null;

    private Listener mListener;
    private Handler mHandler;
    private Surface mSurface;

    final private ExoPlayer mExoPlayer;
    final private RoboTvTrackSelector trackSelector;
    final private RoboTvDataSourceFactory dataSourceFactory;
    final private RoboTvExtractor.Factory extractorFactory;
    final private PositionReference position;
    final private TrickPlayController trickPlayController;

    private Context mContext;

    private boolean mAudioPassthrough;
    private int mChannelConfiguration;

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
        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(context);

        mContext = context;
        mListener = listener;
        mAudioPassthrough = audioPassthrough;
        mAudioPassthrough = audioCapabilities.supportsEncoding(AudioFormat.ENCODING_AC3) && audioPassthrough;
        mChannelConfiguration = Math.min(audioCapabilities.getMaxChannelCount(), wantedChannelConfiguration);

        Log.i(TAG, "audio passthrough: " + (mAudioPassthrough ? "enabled" : "disabled"));

        if(!mAudioPassthrough) {
            Log.i(TAG, "audio channel configuration: " + nameOfChannelConfiguration(mChannelConfiguration));
        }

        mHandler = new Handler();
        position = new PositionReference();

        mVideoRenderer = new MediaCodecVideoRenderer(
                mContext,
                MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT,
                5000, // joining time
                null,
                true,
                mHandler,
                this,
                50);

        mInternalAudioRenderer = new RoboTvAudioRenderer(
                mHandler,
                null,
                mAudioPassthrough,
                mChannelConfiguration);

        mExoAudioRenderer = new MediaCodecAudioRenderer(
                MediaCodecSelector.DEFAULT,
                null,
                true,
                mHandler,
                null,
                audioCapabilities,
                AudioManager.STREAM_MUSIC);

        Renderer[] renderers = { mVideoRenderer, mInternalAudioRenderer, mExoAudioRenderer };

        trackSelector = new RoboTvTrackSelector(mHandler);
        trackSelector.addListener(this);
        trackSelector.setPreferredLanguages(language);

        mExoPlayer = ExoPlayerFactory.newInstance(renderers, trackSelector, new RoboTvLoadControl());
        mExoPlayer.addListener(this);

        dataSourceFactory = new RoboTvDataSourceFactory(position, language, this);
        dataSourceFactory.connect(server);

        extractorFactory = new RoboTvExtractor.Factory(position, this);

        trickPlayController = new TrickPlayController(mHandler, position, mExoPlayer);
    }

    public void release() {
        stop();

        mHandler = null;

        mExoPlayer.removeListener(this);
        mExoPlayer.release();

        dataSourceFactory.release();

        mVideoRenderer = null;
        mInternalAudioRenderer = null;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setStreamVolume(float volume) {
        // TODO - implement stream volume
    }

    public void play() {
        trickPlayController.stop();
        mExoPlayer.setPlayWhenReady(true);
    }

    public void pause() {
        trickPlayController.stop();
        mExoPlayer.setPlayWhenReady(false);
    }

    public boolean isPaused() {
        return !mExoPlayer.getPlayWhenReady();
    }

    public void stop() {
        trackSelector.clearAudioTrack();
        trickPlayController.reset();
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

        sendMessage(mVideoRenderer, C.MSG_SET_SURFACE, mSurface, true);
        return true;
    }

    public void selectAudioTrack(String trackId) {
        trackSelector.selectAudioTrack(trackId);
    }

    public long getStartPosition() {
        return position.getStartPosition();
    }

    public long getEndPosition() {
        return position.getEndPosition();
    }

    public long getCurrentPosition() {
        long timeUs = mExoPlayer.getCurrentPosition() * 1000;
        long pos = position.positionFromTimeUs(timeUs);

        if(pos < position.getStartPosition()) {
            pos = position.getStartPosition();
        }

        return pos;
    }

    public long getBufferedPosition() {
        long timeUs = mExoPlayer.getBufferedPosition() * 1000;
        return position.positionFromTimeUs(timeUs);
    }

    public long getDurationSinceStart() {
        return getCurrentPosition() - getStartPosition();
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

    @TargetApi(23)
    public void setPlaybackParams(PlaybackParams params) {
        Log.d(TAG, "speed: " + params.getSpeed());
        trickPlayController.start(params.getSpeed());
    }

    private void sendMessage(ExoPlayer.ExoPlayerComponent target, int messageType, Object message) {
        sendMessage(target, messageType, message, false);
    }

    private void sendMessage(ExoPlayer.ExoPlayerComponent target, int messageType, Object message, boolean blocking) {
        ExoPlayer.ExoPlayerMessage msg = new ExoPlayer.ExoPlayerMessage(target, messageType, message);

        if(blocking) {
            mExoPlayer.blockingSendMessages(msg);
        }
        else {
            mExoPlayer.sendMessages();
        }
    }

    private static String nameOfChannelConfiguration(int channelConfiguration) {
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
        mListener.onRenderedFirstFrame(surface);
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

            // skip disabled renderers
            if(selection == null) {
                continue;
            }

            Format format = selection.getSelectedFormat();

            // selected audio track
            if(MimeTypes.isAudio(format.sampleMimeType)) {
                mListener.onAudioTrackChanged(format);
            }

            // selected video track
            if(MimeTypes.isVideo(format.sampleMimeType)) {
                mListener.onVideoTrackChanged(format);
            }
        }

    }

    @Override
    public void onStreamError(int status) {
        if(mListener != null) {
            mListener.onStreamError(status);
        }
    }
}
