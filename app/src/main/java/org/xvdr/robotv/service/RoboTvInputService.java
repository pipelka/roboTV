package org.xvdr.robotv.service;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaCodec;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import org.xvdr.extractor.LiveTvSource;
import org.xvdr.robotv.setup.SetupUtils;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.tv.ServerConnection;
import org.xvdr.robotv.tv.StreamBundle;

import java.util.ArrayList;
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
        Session session = new RoboTvSession(this, inputId);
        return session;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Simple session implementation which plays local videos on the application's tune request.
     */
    private class RoboTvSession extends TvInputService.Session implements ExoPlayer.Listener, org.xvdr.msgexchange.Session.Callback, LiveTvSource.Listener, MediaCodecVideoTrackRenderer.EventListener {
        private static final String TAG = "TVSession";

        private static final int RENDERER_COUNT = 2;
        private static final int MIN_BUFFER_MS = 1000;
        private static final int MIN_REBUFFER_MS = 5000;

        private static final int RENDERER_VIDEO = 0;
        private static final int RENDERER_AUDIO = 1;

        private android.os.Handler mHandler;

        private ExoPlayer mPlayer;
        private MediaCodecVideoTrackRenderer mVideoRenderer = null;
        private MediaCodecAudioTrackRenderer mAudioRenderer = null;

        private Surface mSurface;

        private LiveTvSource mSampleSource;

        private Uri mCurrentChannelUri;
        private String mInputId;
        private Runnable mLastTuneRunnable;

        private ServerConnection mConnection = null;
        private Context mContext;

        RoboTvSession(Context context, String inputId) {
            super(context);
            mContext = context;
            mHandler = new android.os.Handler();
            mInputId = inputId;

            mPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT, MIN_BUFFER_MS, MIN_REBUFFER_MS);
            mPlayer.addListener(this);

            // create connection
            mConnection = new ServerConnection("Android TVInputService", SetupUtils.getLanguageISO3(mContext));
            mConnection.addCallback(this);
        }

        @Override
        public void onRelease() {
            if(mConnection != null) {
                mConnection.close();
                mConnection.removeAllCallbacks();
                mConnection = null;
            }

            if (mPlayer != null) {
                mPlayer.removeListener(this);
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }

            mVideoRenderer = null;
            mAudioRenderer = null;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.d(TAG, "onSetSurface()");
            mSurface = surface;

            if(mPlayer == null || mVideoRenderer == null) {
                return true;
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
            Runnable tuneRunnable = new Runnable() {
                @Override
                public void run() {
                    tune(channelUri);
                    startPlayback();
                }
            };

            // remove pending tune request
            mHandler.removeCallbacks(mLastTuneRunnable);
            mLastTuneRunnable = tuneRunnable;

            // post new tune request
            mHandler.postAtFrontOfQueue(tuneRunnable);

            return true;
        }

        private boolean tune(Uri channelUri) {
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

            // stop playback
            mPlayer.stop();

            // open live tv connection
            if(!mConnection.isOpen()) {
                if(!mConnection.open(SetupUtils.getServer(mContext))) {
                    return false;
                }
            }

            // create samplesource
            mSampleSource = new LiveTvSource(mConnection);
            mSampleSource.setListener(this);

            // stream channel
            if(mSampleSource.openStream(uid) == LiveTvSource.ERROR) {
                toastNotification("failed to tune channel");
                return false;
            };

            Log.i(TAG, "successfully switched channel");
            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
        }

        @Override
        public boolean onSelectTrack (int type, String trackId) {
            Log.d(TAG, "onSelectTrack " + trackId);
            StreamBundle bundle = mSampleSource.getStreamBundle();

            if(type == TvTrackInfo.TYPE_AUDIO) {
                int physicalId = Integer.parseInt(trackId);
                int index = bundle.findIndexByPhysicalId(StreamBundle.CONTENT_AUDIO, physicalId);

                if(index == -1) {
                    Log.d(TAG, "track not found !");
                    return false;
                }

                changeAudioTrack(index);
                return true;
            }

            return false;
        }

        private int changeAudioTrack(int index) {
            Log.d(TAG, "changeAudioTrack: " + index);

            mPlayer.setSelectedTrack(RENDERER_AUDIO, index);
            int newIndex = mPlayer.getSelectedTrack(RENDERER_AUDIO);

            if(newIndex == ExoPlayer.TRACK_DISABLED && index != ExoPlayer.TRACK_DEFAULT) {
                mPlayer.setSelectedTrack(RENDERER_AUDIO, ExoPlayer.TRACK_DEFAULT);
                newIndex = mPlayer.getSelectedTrack(RENDERER_AUDIO);
            }

            StreamBundle bundle = mSampleSource.getStreamBundle();
            TvTrackInfo info = bundle.getTrackInfo(StreamBundle.CONTENT_AUDIO, newIndex);

            if (info != null) {
                notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, info.getId());
            }

            return newIndex;
        }

        private boolean startPlayback() {

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

            mPlayer.setSelectedTrack(RENDERER_AUDIO, ExoPlayer.TRACK_DEFAULT);
            mPlayer.setSelectedTrack(RENDERER_VIDEO, ExoPlayer.TRACK_DEFAULT);

            mPlayer.setPlayWhenReady(true);
            return true;
        }

        @Override
        public void onPlayerStateChanged(boolean b, int i) {
            Log.i(TAG, "onPlayerStateChanged " + b + " " + i);

            // we're ready to go
            if(b && i == ExoPlayer.STATE_READY) {
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
            Log.e(TAG, "onPlayerError");
            e.printStackTrace();

            mPlayer.setPlayWhenReady(false);
            mPlayer.setPlayWhenReady(true);
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
                    notification.getU32(); // type
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

        /*@Override
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
        }*/

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
            Log.e(TAG, "onDecoderInitializationError");
            Log.e(TAG, e.getMessage());
        }

        @Override
        public void onCryptoError(MediaCodec.CryptoException e) {
        }

        @Override
        public void onDecoderInitialized(String s, long l, long l1) {
            Log.i(TAG, "onDecoderInitialized");
        }

        @Override
        public void onTracksChanged(StreamBundle bundle) {
            Log.d(TAG, "onTracksChanged");

            final List<TvTrackInfo> tracks = new ArrayList<>(16);

            // create video track
            TvTrackInfo info = bundle.getTrackInfo(StreamBundle.CONTENT_VIDEO, 0);
            if(info != null) {
                tracks.add(info);
            }

            // create audio tracks
            int audioTrackCount = bundle.getStreamCount(StreamBundle.CONTENT_AUDIO);
            Log.d(TAG, "adding " + audioTrackCount + " audio tracks");

            for(int i = 0; i < audioTrackCount; i++) {
                info = bundle.getTrackInfo(StreamBundle.CONTENT_AUDIO, i);
                if(info != null) {
                    Log.d(TAG, "added audio track " + i);
                    tracks.add(info);
                }
            }

            // change tracks
            Log.d(TAG, "changing tracks");
            mPlayer.setSelectedTrack(RENDERER_VIDEO, ExoPlayer.TRACK_DEFAULT);
            final int currentAudioTrack = mPlayer.getSelectedTrack(RENDERER_AUDIO);
            Log.d(TAG, "selected audio track: " + currentAudioTrack);

            // notify about changed tracks
            Log.d(TAG, "notifyTracksChanged");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyTracksChanged(tracks);
                    int newAudioTrack = changeAudioTrack(currentAudioTrack);
                    Log.i(TAG, "new audio track: " + newAudioTrack);
                }
            });
        }
    }
}
