package org.xvdr.robotv.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaCodec;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.widget.Toast;

import org.xvdr.extractor.LiveTvExtractor;
import org.xvdr.robotv.setup.SetupUtils;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.tv.ServerConnection;
import org.xvdr.robotv.tv.StreamBundle;

import java.util.List;

public class RoboTvInputService extends TvInputService {
    static final String TAG = "RoboTvInputService";

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(android.R.style.Theme_DeviceDefault);
    }

    @Override
    public final Session onCreateSession(String inputId) {
        Session session = new SimpleSessionImpl(this, inputId);
        return session;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Simple session implementation which plays local videos on the application's tune request.
     */
    private class SimpleSessionImpl extends TvInputService.Session implements ExoPlayer.Listener, org.xvdr.msgexchange.Session.Callback, LiveTvExtractor.Callback, MediaCodecVideoTrackRenderer.EventListener {
        static final String TAG = "TVSession";
        private static final int RENDERER_COUNT = 2;
        private static final int MIN_BUFFER_MS = 1000;
        private static final int MIN_REBUFFER_MS = 5000;

        private android.os.Handler mHandler;

        private ExoPlayer mPlayer;
        private MediaCodecVideoTrackRenderer mVideoRenderer = null;
        private MediaCodecAudioTrackRenderer mAudioRenderer = null;

        private Surface mSurface;

        private LiveTvExtractor mExtractor;
        private SampleSource mSampleSource;

        private Uri mCurrentChannelUri;
        private String mInputId;

        ServerConnection mConnection = null;
        Context mContext;

        SimpleSessionImpl(Context context, String inputId) {
            super(context);
            mContext = context;
            mHandler = new android.os.Handler();
            mInputId = inputId;

            mPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT, MIN_BUFFER_MS, MIN_REBUFFER_MS);
            mPlayer.addListener(this);
        }

        @Override
        public void onRelease() {
            if (mPlayer != null) {
                mPlayer.removeListener(this);
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }

            if(mConnection != null) {
                mConnection.close();
                mConnection = null;
            }

            mVideoRenderer = null;
            mAudioRenderer = null;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.d(TAG, "onSetSurface()");
            mSurface = surface;

            if(mPlayer == null || mVideoRenderer == null) {
                return false;
            }

            mPlayer.sendMessage(mVideoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);

            return true;
        }

        @Override
        public void onSurfaceChanged (int format, int width, int height) {
            Log.i(TAG, "surface changed: " + width + "x" + height + " format: " + format);
        }

        @Override
        public void onSetStreamVolume(float volume) {
            if (mAudioRenderer != null) {
                mPlayer.sendMessage(mAudioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, volume);
            }
        }


        @Override
        public boolean onTune(final Uri channelUri) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(tune(channelUri)) {
                        startPlayback();
                    }
                }
            });

            return true;
        }

        synchronized boolean tune(Uri channelUri) {
            Log.i(TAG, "onTune: " + channelUri);

            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
            String[] projection = {TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID};
            int uid = 0;

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(channelUri, projection, null, null, null);
                if (cursor == null || cursor.getCount() == 0) {
                    toastNotification("channel not found.");
                    return false;
                }
                cursor.moveToNext();
                uid = cursor.getInt(0);
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            mCurrentChannelUri = channelUri;

            // create live tv connection
            if(mConnection == null) {
                mConnection = new ServerConnection("Android TVInputService", SetupUtils.getLanguageISO3(mContext));
                if(!mConnection.open(SetupUtils.getServer(mContext))) {
                    mConnection = null;
                    return false;
                }
            }

            // remove callbacks
            mConnection.removeAllCallbacks();

            // create extractor / samplesource
            mExtractor = new LiveTvExtractor();
            mExtractor.setCallback(this);

            mSampleSource = new ExtractorSampleSource(
                    channelUri,
                    mExtractor.dataSource(), // this is just a dummy data source
                    new DefaultAllocator(512), // dummy allocator
                    0, // the data source doesn't read any data
                    mExtractor);

            mConnection.addCallback(this);
            mConnection.addCallback(mExtractor);

            mExtractor.setChannelUri(mCurrentChannelUri);

            // stream channel
            Packet req = mConnection.CreatePacket(ServerConnection.XVDR_CHANNELSTREAM_OPEN, ServerConnection.XVDR_CHANNEL_REQUEST_RESPONSE);
            req.putU32(uid);
            req.putS32(50); // priority
            req.putU8((short)0); // start with IFrame

            Packet resp = mConnection.transmitMessage(req);
            if(resp == null) {
                toastNotification("unable to tune channel (no response from server)");
                return false;
            }

            long status = resp.getU32();

            if(status == 0) {
                Log.i(TAG, "successfully switched channel");
            }
            else {
                toastNotification("failed to tune channel (status: " + status + ")");
                return false;
            }

            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // The sample content does not have caption. Nothing to do in this sample input.
            // NOTE: If the channel has caption, the implementation should turn on/off the caption
            // based on {@code enabled}.
            // For the example implementation for the case, please see {@link RichTvInputService}.
        }

        @Override
        synchronized public boolean onSelectTrack (int type, String trackId) {
            if(type == TvTrackInfo.TYPE_AUDIO) {
                changeAudioTrack(trackId);
                return true;
            }

            return false;
        }

        synchronized public void changeAudioTrack(String trackId) {
            Log.d(TAG, "changeAudioTrack: " + trackId);
            if(mExtractor.getCurrentAudioTrackId().equals(trackId)) {
                return;
            }

            mPlayer.setPlayWhenReady(false);
            mPlayer.setSelectedTrack(1, ExoPlayer.TRACK_DISABLED);

            String audioTrackId = selectAudioTrack(trackId);

            Log.d(TAG, "new audio track: " + audioTrackId);

            mPlayer.setSelectedTrack(1, ExoPlayer.TRACK_DEFAULT);
            mPlayer.setPlayWhenReady(true);

            notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, audioTrackId);
        }

        @Override
        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            Log.d(TAG, "onKeyLongPress:" + keyCode);
            return false;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            Log.d(TAG, "onKeyDown:" + keyCode);
            return false;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            Log.d(TAG, "onKeyUp:" + keyCode);
            return false;
        }

        private synchronized boolean startPlayback() {
            if(mPlayer.getPlaybackState() == ExoPlayer.STATE_READY) {
                mPlayer.stop();
                mPlayer.seekTo(0);
            }

            mVideoRenderer = new MediaCodecVideoTrackRenderer(
                    mContext,
                    mSampleSource,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
                    5000,
                    mHandler,
                    this,
                    20);

            mAudioRenderer = new MediaCodecAudioTrackRenderer(mSampleSource);

            if(mSurface != null) {
                mPlayer.sendMessage(mVideoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);
            }

            mPlayer.prepare(mVideoRenderer, mAudioRenderer);

            mPlayer.setSelectedTrack(0, ExoPlayer.TRACK_DEFAULT);
            mPlayer.setSelectedTrack(1, ExoPlayer.TRACK_DEFAULT);

            mPlayer.setPlayWhenReady(true);
            return true;
        }

        @Override
        public void onPlayerStateChanged(boolean b, int i) {
            Log.i(TAG, "onPlayerStateChanged " + b + " " + i);
            if(b && i == 4) {
                notifyVideoAvailable();
            }
        }

        // ExoPlayer.Listener implementation

        @Override
        public void onPlayWhenReadyCommitted() {
            Log.i(TAG, "onPlayWhenReadyCommitted");
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            e.printStackTrace();
        }

        // org.xvdr.msgexchange.Session.Callback implementation

        @Override
        public void onNotification(Packet notification) {
            // process only STATUS messages
            if(notification.getType() != ServerConnection.XVDR_CHANNEL_STATUS) {
                return;
            }

            int id = notification.getMsgID();
            switch(id) {
                case ServerConnection.XVDR_STATUS_MESSAGE:
                    int type = (int) notification.getU32();
                    toastNotification(notification.getString());
                    break;
                case ServerConnection.XVDR_STATUS_RECORDING:
                    notification.getU32();
                    int on = (int) notification.getU32();
                    String recname = notification.getString();
                    String message = "Recording '" + recname + "' ";
                    message += (on == 1) ? "started" : "finished";
                    toastNotification(message);
                    break;
            }
        }

        @Override
        public void onDisconnect() {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            toastNotification("Connection to backend lost");
        }

        @Override
        public void onReconnect() {
            toastNotification("Connection restored");

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mConnection.login();
                    onTune(mCurrentChannelUri);
                }
            });
        }

        private void toastNotification(final String message) {
            Log.i(TAG, message);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private String selectAudioTrack(String id) {
            Log.d(TAG, "selectAudioTrack: " + id);
            String currentAudioTrackId = mExtractor.getCurrentAudioTrackId();

            if(currentAudioTrackId.equals(id)) {
                return currentAudioTrackId;
            }

            String newAudioTrack = mExtractor.selectAudioTrack(id);
            if(newAudioTrack.equals(currentAudioTrackId)) {
                return currentAudioTrackId;
            }

            return newAudioTrack;
        }

        // XVDRExtractor.Callback implementation

        @Override
        public void onTracksChanged(final List<TvTrackInfo> tracks, final StreamBundle streamBundle) {
            Log.d(TAG, "onTracksChanged");

            // notify about changed tracks
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyTracksChanged(tracks);
                    notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, mExtractor.getCurrentAudioTrackId());
                }
            });

            // update channel properties
            StreamBundle.Stream videoStream = streamBundle.getVideoStream();

            if(videoStream == null) {
                Log.e(TAG, "videostream not found !");
                return;
            }

            ContentValues values = new ContentValues();

            if(videoStream.height == 720) {
                values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_720P);
            }
            if(videoStream.height > 720 && videoStream.height <= 1080) {
                values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_1080I);
            }
            else if(videoStream.height == 2160) {
                values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_2160P);
            }
            else if(videoStream.height == 4320) {
                values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_4320P);
            }
            else {
                values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_576I);
            }

            if(mContext.getContentResolver().update(streamBundle.getChannelUri(), values, null, null) != 1) {
                Log.e(TAG, "unable to update channel properties");
            }
        }

        @Override
        public void onAudioTrackChanged(String audioTrackId) {
            changeAudioTrack(audioTrackId);
        }

        @Override
        public void onVideoTrackChanged() {
            mPlayer.setPlayWhenReady(false);
            mPlayer.setSelectedTrack(0, ExoPlayer.TRACK_DISABLED);
            mPlayer.setSelectedTrack(0, ExoPlayer.TRACK_DEFAULT);
            mPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onDroppedFrames(int i, long l) {
            Log.i(TAG, "onDroppedFrames()");
        }

        @Override
        public void onVideoSizeChanged(int i, int i1, int i2, float v) {
            Log.i(TAG, "onVideoSizeChanged" + i + " " + i1 + " " + i2 + " " + v);
        }

        @Override
        public void onDrawnToSurface(Surface surface) {
            Log.i(TAG, "onDrawnToSurface()");
        }

        @Override
        public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {

        }

        @Override
        public void onCryptoError(MediaCodec.CryptoException e) {

        }

        @Override
        public void onDecoderInitialized(String s, long l, long l1) {

        }
    }
}
