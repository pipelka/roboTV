package org.robotv.tv;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.media.PlaybackParams;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.Format;

import org.robotv.dataservice.NotificationHandler;
import org.robotv.player.Player;
import org.robotv.player.StreamBundle;
import org.robotv.robotv.R;
import org.robotv.client.Connection;
import org.robotv.setup.SetupUtils;
import org.robotv.sync.SyncUtils;

import java.util.ArrayList;
import java.util.List;

class RoboTvSession extends TvInputService.Session implements Player.Listener {

    private static final String TAG = "TVSession";

    private Uri mCurrentChannelUri;

    private final Player mPlayer;
    private final TvInputService mContext;

    private final Handler mHandler;
    private final NotificationHandler mNotification;

    private class TuneRunnable implements Runnable {
        private final Uri mChannelUri;

        TuneRunnable(Uri channelUri) {
            mChannelUri = channelUri;
        }

        @Override
        public void run() {
            tune(mChannelUri);
        }
    }

    private final ContentResolver mContentResolver;

    RoboTvSession(TvInputService context/*, String inputId*/) {
        super(context);

        mContext = context;
        mContentResolver =  mContext.getContentResolver();
        mNotification = new NotificationHandler(mContext);
        mHandler = new Handler();

        // player init
        mPlayer = new Player(
            mContext,
            SetupUtils.getServer(mContext),                       // Server
            SetupUtils.getLanguageISO3(mContext),                 // Language
            this,                                          // Listener
            SetupUtils.getPassthrough(mContext),                  // AC3 passthrough
            SetupUtils.getTunneledVideoPlaybackEnabled(mContext)
        );
    }

    @Override
    public void onRelease() {
        Log.i(TAG, "release");

        mPlayer.release();
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        if(mPlayer == null) {
            return false;
        }

        if(surface == null) {
            Log.i(TAG, "set null surface");
            mPlayer.stop();
            return true;
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
        tune(channelUri);
        return true;
    }

    private void scheduleRetune() {
        // post re-tune request
        mHandler.postDelayed(new TuneRunnable(mCurrentChannelUri), 10000);
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
        notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL);
        mNotification.error(getResources().getString(R.string.connection_lost));

        scheduleRetune();
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
        notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
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
                scheduleRetune();
                break;

            default:
                mNotification.error(getResources().getString(R.string.failed_tune));
                break;
        }
    }

    private void tune(Uri channelUri) {
        if(mPlayer == null) {
            Log.d(TAG, "tune: mPlayer == null ?");
            return;
        }

        Log.i(TAG, "onTune: " + channelUri);

        // create channel placeholder
        SyncUtils.ChannelHolder holder = new SyncUtils.ChannelHolder();

        // get information (id's) of the channel
        if(!SyncUtils.getChannelInfo(mContentResolver, channelUri, holder)) {
            mNotification.error(getResources().getString(R.string.channel_not_found));
            return;
        }

        // set current channel uri
        mCurrentChannelUri = channelUri;

        // create roboTV live channel uri
        Uri uri = Player.createLiveUri(holder.channelUid);

        // start playback
        mPlayer.open(uri);
        mPlayer.play();

        Log.i(TAG, "successfully switched channel");
    }

    private Resources getResources() {
        return mContext.getResources();
    }
}
