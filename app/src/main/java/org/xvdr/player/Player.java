package org.xvdr.player;

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
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.PriorityHandlerThread;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.player.audio.RoboTvAudioRenderer;
import org.xvdr.player.extractor.RoboTvExtractor;
import org.xvdr.player.source.RoboTvDataSourceFactory;
import org.xvdr.player.trackselection.RoboTvTrackSelector;
import org.xvdr.player.video.ShieldVideoRenderer;
import org.xvdr.player.video.VideoRendererFactory;
import org.xvdr.robotv.client.StreamBundle;

import java.io.IOException;

public class Player implements ExoPlayer.EventListener, VideoRendererEventListener, RoboTvExtractor.Listener, RoboTvDataSourceFactory.Listener, AudioRendererEventListener {

    private static final String TAG = "Player";

    public final static int CHANNELS_DEFAULT = 0;
    public final static int CHANNELS_STEREO = 2;
    public final static int CHANNELS_SURROUND = 4;
    public final static int CHANNELS_DIGITAL51 = 6;

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

    private Renderer videoRenderer = null;
    private Renderer internalAudioRenderer = null;
    private Renderer exoAudioRenderer = null;

    private Listener listener;
    private PriorityHandlerThread handlerThread;
    private Handler handler;
    private Surface surface;

    final private ExoPlayer player;
    final private RoboTvTrackSelector trackSelector;
    final private RoboTvDataSourceFactory dataSourceFactory;
    final private RoboTvExtractor.Factory extractorFactory;
    final private PositionReference position;
    final private TrickPlayController trickPlayController;

    private Context context;

    private boolean audioPassthrough;
    private int channelConfiguration;

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
        this(
            context,
            server,
            language,
            listener,
            audioPassthrough,
            wantedChannelConfiguration,
            new RoboTvLoadControl()
        );
    }

    public Player(Context context, String server, String language, Listener listener, boolean audioPassthrough, int wantedChannelConfiguration, LoadControl loadControl) throws IOException {
        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(context);

        this.context = context;
        this.listener = listener;
        this.audioPassthrough = audioPassthrough;
        this.audioPassthrough = audioCapabilities.supportsEncoding(AudioFormat.ENCODING_AC3) && audioPassthrough;
        this.channelConfiguration = Math.min(audioCapabilities.getMaxChannelCount(), wantedChannelConfiguration);

        Log.i(TAG, "audio passthrough: " + (this.audioPassthrough ? "enabled" : "disabled"));

        if(!this.audioPassthrough) {
            Log.i(TAG, "audio channel configuration: " + nameOfChannelConfiguration(channelConfiguration));
        }

        handlerThread = new PriorityHandlerThread("roboTV:player", PriorityHandlerThread.NORM_PRIORITY);
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());

        position = new PositionReference();

        videoRenderer = VideoRendererFactory.create(context, handler, this);

        internalAudioRenderer = new RoboTvAudioRenderer(
            handler,
            null,
            this.audioPassthrough,
            channelConfiguration);

        // codecSelector disabling MPEG audio (handled by RoboTvAudioDecoder)
        MediaCodecSelector codecSelector = new MediaCodecSelector() {
            @Override
            public MediaCodecInfo getDecoderInfo(String mimeType, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
                if(mimeType.equals(MimeTypes.AUDIO_MPEG)) {
                    return null;
                }

                if(mimeType.equals(MimeTypes.AUDIO_AC3) && !Player.this.audioPassthrough) {
                    return null;
                }

                return MediaCodecUtil.getDecoderInfo(mimeType, requiresSecureDecoder);
            }

            @Override
            public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                return MediaCodecUtil.getPassthroughDecoderInfo();
            }
        };

        exoAudioRenderer = new MediaCodecAudioRenderer(
            codecSelector,
            null,
            true,
            handler,
            null,
            audioCapabilities,
            AudioManager.STREAM_MUSIC);

        Renderer[] renderers = {videoRenderer, internalAudioRenderer, exoAudioRenderer};

        trackSelector = new RoboTvTrackSelector();
        trackSelector.setParameters(new RoboTvTrackSelector.Parameters().withPreferredAudioLanguage(language));

        player = ExoPlayerFactory.newInstance(renderers, trackSelector, loadControl);
        player.addListener(this);

        dataSourceFactory = new RoboTvDataSourceFactory(position, language, this);
        dataSourceFactory.connect(server);

        extractorFactory = new RoboTvExtractor.Factory(position, this);

        trickPlayController = new TrickPlayController(handler, position, player);
    }

    public void release() {
        stop();

        handler = null;

        player.removeListener(this);
        player.release();

        dataSourceFactory.release();

        videoRenderer = null;
        internalAudioRenderer = null;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
        sendMessage(videoRenderer, C.MSG_SET_SURFACE, surface, true);
    }

    public void setStreamVolume(float volume) {
        sendMessage(internalAudioRenderer, C.MSG_SET_VOLUME, volume);
        sendMessage(exoAudioRenderer, C.MSG_SET_VOLUME, volume);
    }

    public void play() {
        trickPlayController.stop();
        player.setPlayWhenReady(true);
    }

    public void pause() {
        trickPlayController.stop();
        player.setPlayWhenReady(false);
    }

    public boolean isPaused() {
        return !player.getPlayWhenReady();
    }

    public void stop() {
        trickPlayController.reset();
        player.stop();
        trackSelector.clearAudioTrack();
        position.reset();
    }

    public boolean open(Uri uri) {
        stop();

        MediaSource source = new ExtractorMediaSource(
            uri,
            dataSourceFactory,
            extractorFactory,
            handler, null
        );

        player.prepare(source);

        sendMessage(videoRenderer, C.MSG_SET_SURFACE, surface, true);
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
        long timeUs = player.getCurrentPosition() * 1000;
        long startPos = position.getStartPosition();
        long endPos = position.getEndPosition();

        long pos = Math.max(position.positionFromTimeUs(timeUs), startPos);

        // clamp to end position (if we already have a valid endposition)
        if(endPos > startPos) {
            return Math.min(pos, endPos);
        }

        return pos;
    }

    public long getBufferedPosition() {
        long timeUs = player.getBufferedPosition() * 1000;
        return position.positionFromTimeUs(timeUs);
    }

    public long getDurationSinceStart() {
        return getCurrentPosition() - getStartPosition();
    }

    public long getDuration() {
        return position.getDuration();
    }

    public int getPlaybackState() {
        return player.getPlaybackState();
    }

    public void seek(long position) {
        long p = this.position.timeUsFromPosition(Math.max(position, this.position.getStartPosition()));

        Log.d(TAG, "seek position   : " + (position / 1000) + " sec");
        player.seekTo(p / 1000);
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
            player.blockingSendMessages(msg);
        }
        else {
            player.sendMessages();
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
        listener.onPlayerStateChanged(playWhenReady, playbackState);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        listener.onPlayerError(error);
    }

    @Override
    public void onTracksChanged(StreamBundle bundle) {
        listener.onTracksChanged(bundle);
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
        if(trickPlayController.activated()) {
            trickPlayController.postTick();
        }

        listener.onRenderedFirstFrame(surface);
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
    }

    @Override
    public void onDisconnect() {
        listener.onDisconnect();
    }

    @Override
    public void onReconnect() {
        listener.onReconnect();
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
        if(listener == null) {
            return;
        }

        for(int i = 0; i < trackSelectionArray.length; i++) {
            TrackSelection selection = trackSelectionArray.get(i);

            // skip disabled renderers
            if(selection == null) {
                continue;
            }

            Format format = selection.getSelectedFormat();

            // selected audio track
            if(MimeTypes.isAudio(format.sampleMimeType)) {
                listener.onAudioTrackChanged(format);
            }

            // selected video track
            if(MimeTypes.isVideo(format.sampleMimeType)) {
                listener.onVideoTrackChanged(format);
            }
        }
    }

    @Override
    public void onStreamError(int status) {
        if(listener != null) {
            listener.onStreamError(status);
        }
    }

    @Override
    public void onAudioEnabled(DecoderCounters counters) {
    }

    @Override
    public void onAudioSessionId(int audioSessionId) {
    }

    @Override
    public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
    }

    @Override
    public void onAudioInputFormatChanged(Format format) {
        listener.onAudioTrackChanged(format);
    }

    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {
    }

}
