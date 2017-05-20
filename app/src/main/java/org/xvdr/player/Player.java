package org.xvdr.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.ConditionVariable;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.player.extractor.RoboTvExtractor;
import org.xvdr.player.source.RoboTvDataSourceFactory;
import org.xvdr.player.trackselection.RoboTvTrackSelector;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.StreamBundle;

import java.io.IOException;

public class Player implements ExoPlayer.EventListener, RoboTvExtractor.Listener, RoboTvDataSourceFactory.Listener, AudioRendererEventListener, VideoRendererEventListener {

    private static final String TAG = "Player";

    public interface Listener  {

        void onPlayerStateChanged(boolean playWhenReady, int playbackState);

        void onPlayerError(Exception e);

        void onDisconnect();

        void onTracksChanged(StreamBundle bundle);

        void onAudioTrackChanged(Format format);

        void onVideoTrackChanged(Format format);

        void onRenderedFirstFrame();

        void onStreamError(int status);
    }

    private Listener listener;
    private Handler handler;

    final private SimpleExoPlayer player;
    final private RoboTvTrackSelector trackSelector;
    final private RoboTvDataSourceFactory dataSourceFactory;
    final private RoboTvExtractor.Factory extractorFactory;
    final private PositionReference position;
    final private TrickPlayController trickPlayController;
    final private ConditionVariable openConditionVariable;
    final private String server;

    static public Uri createLiveUri(int channelUid) {
        return Uri.parse("robotv://livetv/" + channelUid);
    }

    static public Uri createRecordingUri(String recordingId, long position) {
        return Uri.parse("robotv://recording/" + recordingId + "?position=" + position);
    }

    public Player(Context context, String server, String language, Listener listener) throws IOException {
        this(context, server, language, listener, false);
    }

    public Player(Context context, String server, String language, Listener listener, boolean audioPassthrough) throws IOException {
        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(context);

        this.listener = listener;
        this.server = server;
        boolean passthrough = audioCapabilities.supportsEncoding(AudioFormat.ENCODING_AC3) && audioPassthrough;

        Log.i(TAG, "audio passthrough: " + (passthrough ? "enabled" : "disabled"));

        openConditionVariable = new ConditionVariable();
        handler = new Handler();

        position = new PositionReference();

        trackSelector = new RoboTvTrackSelector();
        trackSelector.setParameters(new RoboTvTrackSelector.Parameters().withPreferredAudioLanguage(language));
        trackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context));

        player = ExoPlayerFactory.newSimpleInstance(
                new RoboTvRenderersFactory(context, audioPassthrough),
                trackSelector,
                new RoboTvLoadControl()
        );

        player.addListener(this);
        player.setVideoDebugListener(this);

        dataSourceFactory = new RoboTvDataSourceFactory(position, language, this);
        extractorFactory = new RoboTvExtractor.Factory(position, this);
        trickPlayController = new TrickPlayController(handler, position, player);
    }

    public void release() {
        stop();

        handler = null;

        player.removeListener(this);
        player.release();

        dataSourceFactory.release();
    }

    public void setSurface(Surface surface) {
        player.setVideoSurface(surface);
    }

    public void setStreamVolume(float volume) {
        player.setVolume(volume);
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

    public void open(Uri uri) {
        stop();

        try {
            dataSourceFactory.connect(server);
        }
        catch (IOException e) {
            if(listener != null) {
                listener.onStreamError(Connection.STATUS_NORESPONSE);
            }

            e.printStackTrace();
            return;
        }

        MediaSource source = new ExtractorMediaSource(
                uri,
                dataSourceFactory,
                extractorFactory,
                handler, null
        );

        player.prepare(source);
    }

    public void openSync(Uri uri) {
        Log.d(TAG, "open sync: " + uri.toString());
        openConditionVariable.close();

        open(uri);

        openConditionVariable.block(5000);
        Log.d(TAG, "open sync - done");
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

    public Connection getConnection() {
        return dataSourceFactory.getConnection();
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
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
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
    public void onDroppedFrames(int count, long elapsedMs) {
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        if(trickPlayController.activated()) {
            trickPlayController.postTick();
            return;
        }

        listener.onRenderedFirstFrame();
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
    }

    @Override
    public void onDisconnect() {
        listener.onDisconnect();
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
    public void onServerTuned(int status) {
        openConditionVariable.open();
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
