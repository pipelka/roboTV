package org.xvdr.robotv.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.media.PlaybackParams;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.Format;

import org.xvdr.player.Player;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.StreamBundle;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.tv.TrackInfoMapper;
import org.xvdr.sync.SyncChannelEPGTask;
import org.xvdr.sync.SyncUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class RoboTvSession extends TvInputService.Session implements Player.Listener {

    private static final String TAG = "TVSession";

    private Uri mCurrentChannelUri;
    private String mInputId;

    private Player mPlayer;
    private TvInputService mContext;

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

    private Runnable mUpdateEPG = new Runnable() {
        @Override
        public void run() {
            Connection connection = new Connection("Channel EPG update", "", false);
            if(connection.open(SetupUtils.getServer(mContext))) {
                SyncChannelEPGTask task = new SyncChannelEPGTask(connection, mContext, true);
                task.execute(mCurrentChannelUri);
            }
        }
    };

    private ContentResolver mContentResolver;

    RoboTvSession(TvInputService context, String inputId) {
        super(context);
        mContext = context;
        mInputId = inputId;
        mContentResolver =  mContext.getContentResolver();

        mNotification = new NotificationHandler(mContext);

        mHandler = new Handler();

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
        Log.d(TAG, "postTune: " + channelUri.toString());
        postTune(channelUri, 0);
        return true;
    }

    private void postTune(final Uri channelUri, long delayMillis) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && SetupUtils.getTimeshiftEnabled(mContext)) {
            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
        }

        // remove pending tune request
        mHandler.removeCallbacks(mTune);

        if(channelUri != null) {
            mTune.setChannelUri(channelUri);
        }

        // post new tune request
        mHandler.postDelayed(mTune, delayMillis);
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
        if(playWhenReady && playbackState == com.google.android.exoplayer2.Player.STATE_BUFFERING) {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
        }

        if(playWhenReady && playbackState == com.google.android.exoplayer2.Player.STATE_READY) {
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

        postTune(null, 10 * 1000);
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
        Log.d(TAG, "onAudioTrackChanged: " + format.id);
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
    public void onRenderedFirstFrame() {
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

            case Connection.STATUS_CONNECTION_FAILED:
                mNotification.notify(getResources().getString(R.string.failed_connect));
                postTune(null, 10 * 1000);
                break;

            default:
                mNotification.error(getResources().getString(R.string.failed_tune));
                break;
        }
    }

    private boolean tune(Uri channelUri) {
        if(mPlayer == null) {
            Log.d(TAG, "tune: mPlayer == null ?");
            return false;
        }

        Log.i(TAG, "onTune: " + channelUri);

        // create chennl placeholder
        SyncUtils.ChannelHolder holder = new SyncUtils.ChannelHolder();

        // get information (id's) of the channel
        if(!SyncUtils.getChannelInfo(mContentResolver, channelUri, holder)) {
            mNotification.error(getResources().getString(R.string.channel_not_found));
            return false;
        }

        // set current channel uri
        mCurrentChannelUri = channelUri;

        // create roboTV live channel uri
        Uri uri = Player.createLiveUri(holder.channelUid);

        // start playback
        mPlayer.openSync(uri);
        mPlayer.play();

        // sync EPG of this channel after 5 seconds
        mHandler.removeCallbacks(mUpdateEPG);
        mHandler.postDelayed(mUpdateEPG, 1000);

        Log.i(TAG, "successfully switched channel");
        return true;
    }

    private Resources getResources() {
        return mContext.getResources();
    }
}
