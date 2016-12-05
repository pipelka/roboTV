package org.xvdr.robotv.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.PlaybackParams;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;

import org.xvdr.player.Player;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.StreamBundle;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.tv.TrackInfoMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class RoboTvSession extends TvInputService.Session implements Player.Listener {

    private static final String TAG = "TVSession";

    private Uri mCurrentChannelUri;
    private String mInputId;

    private Player mPlayer;
    private TvInputService mContext;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private NotificationHandler mNotification;

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

        mNotification = new NotificationHandler(mContext);

        mHandlerThread = new HandlerThread("robotv:eventhandler", android.os.Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        // player init
        try {
            mPlayer = new Player(
                mContext,
                SetupUtils.getServer(mContext),                 // Server
                SetupUtils.getLanguageISO3(mContext),           // Language
                this,                                           // Listener
                SetupUtils.getPassthrough(mContext)             // AC3 passthrough
            );
        }
        catch(IOException e) {
            mNotification.error(getResources().getString(R.string.connect_unable));
            e.printStackTrace();
        }
    }

    @Override
    public void onRelease() {
        if(mPlayer != null) {
            mPlayer.release();
        }

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
        if(mPlayer != null) {
            mPlayer.setStreamVolume(volume);
        }
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
        mPlayer.pause();
    }

    @Override
    public void onTimeShiftResume() {
        mPlayer.play();
    }

    @Override
    public long onTimeShiftGetStartPosition() {
        if(mPlayer == null) {
            return System.currentTimeMillis();
        }

        return mPlayer.getStartPosition();
    }

    @Override
    public long onTimeShiftGetCurrentPosition() {
        long currentPos = System.currentTimeMillis();

        if(mPlayer == null) {
            return currentPos;
        }

        return mPlayer.getCurrentPosition();
    }

    @Override
    public void onTimeShiftSeekTo(long timeMs) {
        mPlayer.seek(timeMs);
    }

    @Override
    public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
        mPlayer.setPlaybackParams(params);
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {
    }

    @Override
    public boolean onSelectTrack(int type, String trackId) {
        if(type == TvTrackInfo.TYPE_AUDIO) {
            mPlayer.selectAudioTrack(trackId);
        }

        return true;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if(playWhenReady && playbackState == ExoPlayer.STATE_BUFFERING) {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
        }

        if(playWhenReady && playbackState == ExoPlayer.STATE_READY) {
            notifyVideoAvailable();
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
                               0);

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
    public void onAudioTrackChanged(Format format) {
        notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, format.id);
    }

    @Override
    public void onVideoTrackChanged(Format format) {
        ContentValues values = new ContentValues();

        int height = format.height;

        if(height == 720) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_720P);
        }

        if(height > 720 && height <= 1080) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_1080I);
        }
        else if(height == 2160) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_2160P);
        }
        else if(height == 4320) {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_4320P);
        }
        else {
            values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_576I);
        }

        if(mContentResolver.update(mCurrentChannelUri, values, null, null) != 1) {
            Log.e(TAG, "unable to update channel properties");
        }

        notifyTrackSelected(TvTrackInfo.TYPE_VIDEO, format.id);
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        notifyVideoAvailable();
    }

    @Override
    public void onStreamError(int status) {
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
    }

    private boolean tune(Uri channelUri) {
        if(mPlayer == null) {
            return false;
        }

        Log.i(TAG, "onTune: " + channelUri);

        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
        String[] projection = {TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID};
        int channelUid = 0;

        Cursor cursor = null;

        try {
            cursor = mContentResolver.query(channelUri, projection, null, null, null);

            if(cursor == null || cursor.getCount() == 0) {
                mNotification.error(getResources().getString(R.string.channel_not_found));
                return false;
            }

            cursor.moveToNext();
            channelUid = cursor.getInt(0);
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        mCurrentChannelUri = channelUri;

        Uri uri = Player.createLiveUri(channelUid);

        mPlayer.open(uri);
        mPlayer.play();

        Log.i(TAG, "successfully switched channel");
        return true;
    }

    private Resources getResources() {
        return mContext.getResources();
    }
}
