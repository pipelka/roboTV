package org.xvdr.robotv.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.media.PlaybackParams;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.PriorityHandlerThread;

import org.xvdr.extractor.LiveTvPlayer;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.StreamBundle;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.tv.TrackInfoMapper;

import java.util.ArrayList;
import java.util.List;

class RoboTvSession extends TvInputService.Session implements LiveTvPlayer.Listener {

    private static final String TAG = "TVSession";

    private Uri mCurrentChannelUri;
    private String mInputId;

    private LiveTvPlayer mPlayer;
    private TvInputService mContext;

    private PriorityHandlerThread mHandlerThread;
    private Handler mHandler;
    private NotificationHandler mNotification;

    private Point mDisplaySize = new Point();

    private class TuneRunnable implements Runnable {
        private Uri mChannelUri;

        void setChannelUri(Uri channelUri) {
            mChannelUri = channelUri;
        }

        @Override
        public void run() {
            tune(mChannelUri);
        }
    }

    private TuneRunnable mTune = new TuneRunnable();
    private ContentResolver mContentResolver;

    RoboTvSession(TvInputService context, String inputId) {
        super(context);
        mContext = context;
        mInputId = inputId;
        mContentResolver =  mContext.getContentResolver();

        // get display width / height
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        display.getSize(mDisplaySize);

        // player init
        mPlayer = new LiveTvPlayer(
            mContext,
            SetupUtils.getServer(mContext),                 // XVDR server
            SetupUtils.getLanguageISO3(mContext),           // Language
            this,                                           // Listener
            SetupUtils.getPassthrough(mContext),            // AC3 passthrough
            SetupUtils.getSpeakerConfiguration(mContext));  // channel layout

        mNotification = new NotificationHandler(mContext);

        mHandlerThread = new PriorityHandlerThread("robotv:eventhandler", android.os.Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public void onRelease() {
        mPlayer.release();
        mHandlerThread.interrupt();
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        if(mPlayer == null) {
            return false;
        }

        Log.i(TAG, "set surface");
        mPlayer.setSurface(surface);
        return true;
    }

    @Override
    public void onSurfaceChanged(int format, int width, int height) {
        Log.i(TAG, "surface changed: " + width + "x" + height + " format: " + format);
    }

    @Override
    public void onSetStreamVolume(float volume) {
        mPlayer.setStreamVolume(volume);
    }

    @Override
    public boolean onTune(final Uri channelUri) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && SetupUtils.getTimeshiftEnabled(mContext)) {
            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
        }

        // remove pending tune request
        mHandler.removeCallbacks(mTune);
        mTune.setChannelUri(channelUri);

        // post new tune request
        mHandler.post(mTune);

        return true;
    }

    @Override
    public void onTimeShiftPause() {
        mPlayer.pause(true);
    }

    @Override
    public void onTimeShiftResume() {
        mPlayer.pause(false);
    }

    @Override
    public long onTimeShiftGetStartPosition() {
        long now = System.currentTimeMillis();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return now;
        }

        if(mPlayer == null || mPlayer.getPlaybackState() < ExoPlayer.STATE_BUFFERING) {
            return now;
        }

        long pos = mPlayer.getStartPositionWallclock();

        // minimum timeshift start position ( now - 24hrs )
        long minStartPosition = now - 1000 * 60 * 60 * 24;

        if(pos <= minStartPosition) {
            return minStartPosition;
        }

        return pos;
    }

    @Override
    public long onTimeShiftGetCurrentPosition() {
        long now = System.currentTimeMillis();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return now;
        }

        if(mPlayer == null || mPlayer.getPlaybackState() < ExoPlayer.STATE_BUFFERING) {
            return now;
        }

        return mPlayer.getCurrentPositionWallclock();
    }

    @Override
    public void onTimeShiftSeekTo(long timeMs) {
        mPlayer.seekTo(timeMs);
    }

    @Override
    public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPlayer.setPlaybackSpeed((int) params.getSpeed());
        }
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {
    }

    @Override
    public boolean onSelectTrack(int type, String trackId) {
        return type == TvTrackInfo.TYPE_AUDIO && mPlayer.selectAudioTrack(Integer.parseInt(trackId));

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.i(TAG, "onPlayerStateChanged " + playWhenReady + " " + playbackState);

        if(playWhenReady && playbackState == ExoPlayer.STATE_READY) {
            notifyVideoAvailable();
        }
        else if(playWhenReady && playbackState == ExoPlayer.STATE_BUFFERING) {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
        }
    }

    // Listener implementation

    @Override
    public void onPlayerError(Exception e) {
        mNotification.error(getResources().getString(R.string.player_error));
        Log.e(TAG, "onPlayerError");
        e.printStackTrace();
    }

    @Override
    public void onDisconnect() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && SetupUtils.getTimeshiftEnabled(mContext)) {
            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
        }

        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL);
        mNotification.error(getResources().getString(R.string.connection_lost));
    }

    @Override
    public void onReconnect() {
        mNotification.notify(getResources().getString(R.string.connection_restored));
        onTune(mCurrentChannelUri);
    }

    @Override
    public void onTracksChanged(StreamBundle bundle) {
        final List<TvTrackInfo> tracks = new ArrayList<>(16);

        // create video track (limit surface size to display size)
        TvTrackInfo info = TrackInfoMapper.findTrackInfo(
                               bundle,
                               StreamBundle.CONTENT_VIDEO,
                               0,
                               mDisplaySize.x,
                               mDisplaySize.y
                           );

        if(info != null) {
            tracks.add(info);
        }

        // create audio tracks
        int audioTrackCount = bundle.getStreamCount(StreamBundle.CONTENT_AUDIO);

        for(int i = 0; i < audioTrackCount; i++) {
            info = TrackInfoMapper.findTrackInfo(bundle, StreamBundle.CONTENT_AUDIO, i);

            if(info != null) {
                tracks.add(info);
            }
        }

        notifyTracksChanged(tracks);
    }

    @Override
    public void onAudioTrackChanged(StreamBundle.Stream stream) {
        notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, Integer.toString(stream.physicalId));
    }

    @Override
    public void onVideoTrackChanged(StreamBundle.Stream stream) {
        ContentValues values = new ContentValues();

        if(stream.height == 720) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_720P);
        }

        if(stream.height > 720 && stream.height <= 1080) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_1080I);
        }
        else if(stream.height == 2160) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_2160P);
        }
        else if(stream.height == 4320) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_4320P);
        }
        else {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_576I);
        }

        if(mContentResolver.update(mCurrentChannelUri, values, null, null) != 1) {
            Log.e(TAG, "unable to update channel properties");
        }

        notifyTrackSelected(TvTrackInfo.TYPE_VIDEO, Integer.toString(stream.physicalId));
    }

    @Override
    public void onAudioTrackUnderrun(int i, long l, long l1) {
        Log.e(TAG, "audio track underrun");
    }

    private boolean tune(Uri channelUri) {
        if(mPlayer == null) {
            return false;
        }

        Log.i(TAG, "onTune: " + channelUri);

        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
        String[] projection = {TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID};
        int uid = 0;

        Cursor cursor = null;

        try {
            cursor = mContentResolver.query(channelUri, projection, null, null, null);

            if(cursor == null || cursor.getCount() == 0) {
                mNotification.error(getResources().getString(R.string.channel_not_found));
                return false;
            }

            cursor.moveToNext();
            uid = cursor.getInt(0);
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        mCurrentChannelUri = channelUri;

        // start player
        // stream channel
        String language = SetupUtils.getLanguageISO3(mContext);

        int status = mPlayer.openStream(uid, language);

        if(status != Connection.STATUS_SUCCESS) {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            Log.d(TAG, "status: " + status);

            switch(status) {
                case Connection.STATUS_RECEIVERS_BUSY:
                    mNotification.notify(getResources().getString(R.string.receivers_busy));
                    break;

                case Connection.STATUS_BLOCKED_BY_RECORDING:
                    mNotification.notify(getResources().getString(R.string.blocked_by_recording));
                    break;

                default:
                    mNotification.error(getResources().getString(R.string.failed_tune));
                    break;
            }

            return false;
        }

        Log.i(TAG, "successfully switched channel");
        return true;
    }

    private Resources getResources() {
        return mContext.getResources();
    }
}
