package org.xvdr.robotv.service;

import android.content.ContentValues;
import android.content.Context;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.PriorityHandlerThread;

import org.xvdr.extractor.LiveTvPlayer;
import org.xvdr.extractor.Player;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.R;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.StreamBundle;
import org.xvdr.robotv.tv.TrackInfoMapper;

import java.util.ArrayList;
import java.util.List;

public class RoboTvInputService extends TvInputService {

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(android.R.style.Theme_DeviceDefault);
    }

    @Override
    public final Session onCreateSession(String inputId) {
        Session session = new RoboTvSession(this, inputId);
        session.setOverlayViewEnabled(true);

        return session;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Simple session implementation which plays local videos on the application's tune request.
     */
    private class RoboTvSession extends TvInputService.Session implements Player.Listener {

        private static final String TAG = "TVSession";

        private Uri mCurrentChannelUri;
        private String mInputId;
        private Runnable mLastResetRunnable;

        private LiveTvPlayer mPlayer;
        private Context mContext;

        private PriorityHandlerThread mHandlerThread;
        private Handler mHandler;
        private final Toast mTuningToast;

        private Point mDisplaySize = new Point();

        private class TuneRunnable implements Runnable {
            private Uri mChannelUri;

            public void setChannelUri(Uri channelUri) {
                mChannelUri = channelUri;
            }

            @Override
            public void run() {
                tune(mChannelUri);
            }
        };

        private TuneRunnable mTune = new TuneRunnable();

        RoboTvSession(Context context, String inputId) {
            super(context);
            mContext = context;

            mInputId = inputId;

            mHandlerThread = new PriorityHandlerThread("robotv:eventhandler", android.os.Process.THREAD_PRIORITY_DEFAULT);
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());

            // player init
            mPlayer = new LiveTvPlayer(
                mContext,
                SetupUtils.getServer(mContext),                 // XVDR server
                SetupUtils.getLanguageISO3(mContext),           // Language
                this,                                           // Listener
                SetupUtils.getPassthrough(mContext),            // AC3 passthrough
                SetupUtils.getSpeakerConfiguration(mContext));  // channel layout

            mTuningToast = new Toast(mContext);
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.layout_tuning, null);

            mTuningToast.setView(view);
            mTuningToast.setDuration(Toast.LENGTH_SHORT);
            mTuningToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);

            // get display width / height
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();

            display.getSize(mDisplaySize);
        }

        @Override
        public void onRelease() {

            cancelReset();
            mPlayer.release();
            mHandlerThread.interrupt();
        }

        @Override
        public View onCreateOverlayView() {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.overlayview, null);
            return view;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
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
            return mPlayer.getStartPositionWallclock();
        }

        @Override
        public long onTimeShiftGetCurrentPosition() {
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
                cursor = getContentResolver().query(channelUri, projection, null, null, null);

                if(cursor == null || cursor.getCount() == 0) {
                    errorNotification(getResources().getString(R.string.channel_not_found));
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

            if(mPlayer.openStream(uid, language) != Connection.SUCCESS) {
                errorNotification(getResources().getString(R.string.failed_tune));
                return false;
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
            }

            Log.i(TAG, "successfully switched channel");
            return true;
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
        }

        // Listener implementation

        @Override
        public void onPlayerError(Exception e) {
            toastNotification(getResources().getString(R.string.player_error));
            Log.e(TAG, "onPlayerError");
            e.printStackTrace();

            onTune(mCurrentChannelUri);
        }

        @Override
        public void onNotification(Packet notification) {
            String message;

            // process only STATUS messages
            if(notification.getType() != Connection.XVDR_CHANNEL_STATUS) {
                return;
            }

            int id = notification.getMsgID();
            Log.d(TAG, "notification id: " + id);

            switch(id) {
                case Connection.XVDR_STATUS_MESSAGE:
                    Log.d(TAG, "status message");
                    notification.getU32(); // type
                    message = notification.getString();
                    toastNotification(message);
                    break;

                case Connection.XVDR_STATUS_RECORDING:
                    Log.d(TAG, "recording status");
                    notification.getU32(); // card index
                    int on = (int) notification.getU32(); // on

                    String recname = notification.getString(); // name
                    notification.getString(); // filename

                    message = mContext.getResources().getString(R.string.recording_text) + " ";
                    message += (on == 1) ?
                               mContext.getResources().getString(R.string.recording_started) :
                               mContext.getResources().getString(R.string.recording_finished);

                    toastNotification(recname, message, R.drawable.ic_movie_white_48dp);
                    break;
            }
        }

        @Override
        public void onDisconnect() {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            errorNotification(mContext.getResources().getString(R.string.connection_lost));
        }

        @Override
        public void onReconnect() {
            toastNotification(mContext.getResources().getString(R.string.connection_restored));
            onTune(mCurrentChannelUri);
        }

        private void toastNotification(String message) {
            toastNotification(
                message,
                mContext.getResources().getString(R.string.toast_information),
                R.drawable.ic_info_outline_white_48dp);
        }

        private void toastNotification(String message, String title) {
            toastNotification(message, title, R.drawable.ic_info_outline_white_48dp);
        }

        private void toastNotification(final String message, final String title, final int icon) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    final Toast toast = new Toast(mContext);
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    View view = inflater.inflate(R.layout.layout_toast, null);

                    TextView titleView = (TextView) view.findViewById(R.id.title);
                    titleView.setText(title);

                    TextView messageView = (TextView) view.findViewById(R.id.message);
                    messageView.setText(message);

                    ImageView imageView = (ImageView) view.findViewById(R.id.icon);
                    imageView.setImageResource(icon);

                    toast.setView(view);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.RIGHT | Gravity.BOTTOM, 0, 0);

                    toast.show();
                }
            });
        }

        private void toastTuning(final int state) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(state == ExoPlayer.STATE_READY) {
                        mTuningToast.cancel();
                        return;
                    }

                    mTuningToast.show();
                }
            });
        }

        private void errorNotification(String message) {
            toastNotification(
                message,
                mContext.getResources().getString(R.string.toast_error),
                R.drawable.ic_error_outline_white_48dp);
        }

        @Override
        public void onDrawnToSurface(Surface surface) {
            Log.i(TAG, "onDrawnToSurface()");
            notifyVideoAvailable();
        }

        private void cancelReset() {
            if(mLastResetRunnable == null) {
                return;
            }

            mHandler.removeCallbacks(mLastResetRunnable);
            mLastResetRunnable = null;
        }

        private void scheduleReset() {
            // only for "SHIELD Android TV"
            if(!Build.MODEL.equals("SHIELD Android TV")) {
                return;
            }

            // schedule player reset
            Runnable reset = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "track reset");
                    doReset();
                    scheduleReset();
                }
            };

            cancelReset();

            mHandler.postDelayed(reset, 12 * 60 * 1000); // 12 minutes
            mLastResetRunnable = reset;
        }

        private void doReset() {
            mPlayer.reset();
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
            cancelReset();
            ContentValues values = new ContentValues();

            if(stream.height == 720) {
                values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_720P);
            }

            if(stream.height > 720 && stream.height <= 1080) {
                values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_1080I);
                scheduleReset();
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

            if(mContext.getContentResolver().update(mCurrentChannelUri, values, null, null) != 1) {
                Log.e(TAG, "unable to update channel properties");
            }

            notifyTrackSelected(TvTrackInfo.TYPE_VIDEO, Integer.toString(stream.physicalId));
        }

        @Override
        public void onAudioTrackUnderrun(int i, long l, long l1) {
            Log.e(TAG, "audio track underrun");
        }
    }
}
