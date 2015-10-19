package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorInput;
import com.google.android.exoplayer.extractor.ExtractorOutput;
import com.google.android.exoplayer.extractor.PositionHolder;
import com.google.android.exoplayer.extractor.SeekMap;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xvdr.msgexchange.Packet;
import org.xvdr.msgexchange.Session;
import org.xvdr.robotv.tv.ServerConnection;
import org.xvdr.robotv.tv.StreamBundle;

/**
 * Facilitates the extraction of stream data from the XVDR connection
 */
public final class LiveTvExtractor implements Extractor, SeekMap, Session.Callback {

    public interface Callback {

        void onTracksChanged(List<TvTrackInfo> tracks, StreamBundle streamBundle);

        void onAudioTrackChanged(String audioTrackId);

        void onVideoTrackChanged();

    }

	private static final String TAG = "LiveTvExtractor";

	private static final int TS_STREAM_TYPE_AAC = 0x0F;
	private static final int TS_STREAM_TYPE_ATSC_AC3 = 0x81;
	private static final int TS_STREAM_TYPE_ATSC_E_AC3 = 0x87;
	private static final int TS_STREAM_TYPE_H264 = 0x1B;
	private static final int TS_STREAM_TYPE_ID3 = 0x15;
	private static final int TS_STREAM_TYPE_EIA608 = 0x100; // 0xFF + 1

	private static final long MAX_PTS = 0x1FFFFFFFFL;

	private long firstSampleTimestampUs;
	SparseBooleanArray streamTypes;
	SparseBooleanArray allowedPassthroughStreamTypes;
	SparseArray<ElementaryStreamReader> streamReaders; // Indexed by pid

	private ExtractorOutput output;
	private long timestampOffsetUs;
	private long lastPts;

    int currentAudioPid = 0;
    int mPreviousAudioPid = 0;

    private StreamBundle mBundle;
    private StreamBundle mPreviousBundle;

    Callback mCallback = null;
    Uri mChannelUri;

	public LiveTvExtractor() {
		this(0, null);
	}

	public LiveTvExtractor(long firstSampleTimestampUs, AudioCapabilities audioCapabilities) {
        allowedPassthroughStreamTypes = getPassthroughStreamTypes(audioCapabilities);
        reset();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setChannelUri(Uri channelUri) {
        mChannelUri = channelUri;
    }

	// Extractor implementation.

	@Override
	public void init(ExtractorOutput output) {
		this.output = output;
		output.seekMap(this);
	}

    @Override
    public boolean sniff(ExtractorInput extractorInput) throws IOException, InterruptedException {
        return true;
    }

    @Override
    synchronized public void seek() {
		timestampOffsetUs = 0;
		lastPts = Long.MIN_VALUE;

        synchronized (streamReaders) {
            for(int i = 0; i < streamReaders.size(); i++) {
                streamReaders.valueAt(i).seek();

            }
        }
	}

    synchronized public void reset() {
        streamReaders = new SparseArray<>();
        synchronized (streamReaders) {
            streamReaders.clear();
            timestampOffsetUs = 0;
            lastPts = Long.MIN_VALUE;
            streamTypes = new SparseBooleanArray();
            this.firstSampleTimestampUs = 0;
            lastPts = Long.MIN_VALUE;
            currentAudioPid = 0;
        }
    }

	@Override
    synchronized public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
		return RESULT_CONTINUE;
	}

	// SeekMap implementation.

	@Override
	public boolean isSeekable() {
		return false;
	}

	@Override
	public long getPosition(long timeUs) {
		return 0;
	}

	// Internals.

	long ptsToTimeUs(long pts) {

		// initial timestamp adjustment
		if(lastPts == Long.MIN_VALUE) {
			timestampOffsetUs = firstSampleTimestampUs - pts;
		}

		lastPts = pts;
		return pts + timestampOffsetUs;
	}

	/**
	 * Returns a sparse boolean array of stream types that can be played back based on
	 * {@code audioCapabilities}.
	 */
	private static SparseBooleanArray getPassthroughStreamTypes(AudioCapabilities audioCapabilities) {
		SparseBooleanArray streamTypes = new SparseBooleanArray();

		if(audioCapabilities != null) {
			if(audioCapabilities.supportsEncoding(C.ENCODING_AC3)) {
				streamTypes.put(TS_STREAM_TYPE_ATSC_AC3, true);
			}

			if(audioCapabilities.supportsEncoding(C.ENCODING_E_AC3)) {
				// TODO: Uncomment when Ac3Reader supports enhanced AC-3.
				// streamTypes.put(TS_STREAM_TYPE_ATSC_E_AC3, true);
			}
		}

		return streamTypes;
	}

    synchronized public String selectAudioTrack(String id) {
        Log.d(TAG, "selectAudioTrack: " + id);
        int pid = Integer.parseInt(id);

        if(mBundle == null) {
            return "";
        }

        StreamBundle.Stream stream = mBundle.get(pid);

        if(stream == null) {
            stream = mBundle.getDefaultAudioStream();
        }

        if(stream == null) {
            return "";
        }

        synchronized (streamReaders) {
            streamReaders.remove(currentAudioPid);
        }

        currentAudioPid = 0;
        addStreamReader(stream);

        return Integer.toString(stream.physicalId);
    }

    private boolean checkAudioTrackChanged() {
        // no change
        if(mPreviousBundle == null || mPreviousAudioPid == 0) {
            return false;
        }

        if(currentAudioPid != mPreviousAudioPid) {
            mCallback.onAudioTrackChanged(Integer.toString(currentAudioPid));
            return false;
        }

        StreamBundle.Stream previousAudioStream = mPreviousBundle.get(mPreviousAudioPid);
        if(previousAudioStream == null) {
            return false;
        }

        return !previousAudioStream.equals(mBundle.get(currentAudioPid));
    }

    private boolean checkVideoTrackChanged() {
        // no change
        if(mPreviousBundle == null || mBundle == null) {
            return false;
        }

        StreamBundle.Stream previous = mPreviousBundle.getVideoStream();
        StreamBundle.Stream current = mBundle.getVideoStream();

        return !current.equals(previous);

    }

    synchronized void createStreamReaders(Packet p) {
        List<TvTrackInfo> tracks = new ArrayList<>();

        mPreviousBundle = mBundle;
        mPreviousAudioPid = currentAudioPid;

        mBundle = new StreamBundle(mChannelUri);
		mBundle.updateFromPacket(p);

		for(int i = 0, nsize = mBundle.size(); i < nsize; i++) {
            StreamBundle.Stream stream = mBundle.valueAt(i);

			addStreamReader(stream);

            TvTrackInfo info = stream.getTrackInfo();

            if(info != null) {
                tracks.add(info);
            }
		}

		output.endTracks();

        if(mCallback == null) {
            return;
        }

        mCallback.onTracksChanged(tracks, mBundle);

        if(checkAudioTrackChanged()) {
            Log.d(TAG, "audio track changed");
            mCallback.onAudioTrackChanged(Integer.toString(currentAudioPid));
        }

        if(checkVideoTrackChanged()) {
            Log.d(TAG, "video track changed");
            mCallback.onVideoTrackChanged();
        }
    }

	synchronized void addStreamReader(StreamBundle.Stream stream) {
		ElementaryStreamReader reader = null;

		switch(stream.getMimeType()) {
			case MimeTypes.VIDEO_H264:
				reader = new H264Reader(output.track(TS_STREAM_TYPE_H264), stream);
				streamTypes.put(TS_STREAM_TYPE_H264, true);
				break;

			case MimeTypes.AUDIO_AAC:
				reader = new AdtsReader(output.track(TS_STREAM_TYPE_AAC), stream);
				streamTypes.put(TS_STREAM_TYPE_AAC, true);
				break;

			case MimeTypes.AUDIO_AC3:
				if(allowedPassthroughStreamTypes.get(TS_STREAM_TYPE_ATSC_AC3)) {
					reader = new Ac3Reader(output.track(TS_STREAM_TYPE_ATSC_AC3), stream);
				}
				else {
					reader = new Ac3Decoder(output.track(TS_STREAM_TYPE_ATSC_AC3), stream);
				}
				streamTypes.put(TS_STREAM_TYPE_ATSC_AC3, true);
				break;
		}

		if(reader != null) {
            if(stream.content == StreamBundle.CONTENT_AUDIO && currentAudioPid != 0) {
                return;
            }

            if(stream.content == StreamBundle.CONTENT_AUDIO) {
                currentAudioPid = stream.physicalId;
            }


            Log.i(TAG, "added stream " + stream.physicalId + " (" + stream.type + ")");
            synchronized (streamReaders) {
                streamReaders.put(stream.physicalId, reader);
            }
		}
	}

    synchronized public StreamBundle.Stream getCurrentAudioStream() {
        if(currentAudioPid == 0) {
            return null;
        }

        return mBundle.get(currentAudioPid);
    }

    synchronized public String getCurrentAudioTrackId() {
        return Integer.toString(currentAudioPid);
    }

	// Session.Callback

	@Override
    synchronized public void onNotification(Packet notification) {
		// process STREAMCHANGE notification
		if(notification.getMsgID() == ServerConnection.XVDR_STREAM_CHANGE) {
			createStreamReaders(notification);
			return;
		}

		// exit if it's not a MUXPKT
		if(notification.getMsgID() != ServerConnection.XVDR_STREAM_MUXPKT) {
			return;
		}

		// read pid of packet
		int pid = notification.getU16();

        ElementaryStreamReader reader = null;
        synchronized (streamReaders) {
             reader = streamReaders.get(pid);
        }

		if(reader == null) {
			return;
		}

		// read packet properties

		long pts = notification.getS64();
		long dts = notification.getS64();
		int duration = (int) notification.getU32();
		int length = (int) notification.getU32();
        int frameType = notification.getClientID();
        long timeUs = ptsToTimeUs(pts);

        // skip empty packet
        if(length == 0) {
            return;
        }

		// read buffer
		byte[] buffer = new byte[length];
		notification.readBuffer(buffer, 0, length);

        // push buffer to reader
        reader.consume(new ParsableByteArray(buffer), timeUs, (frameType == 2), duration);
	}

	@Override
	public void onDisconnect() {
	}

	@Override
	public void onReconnect() {
	}

    public DataSource dataSource() {
        return new DataSource() {
            @Override
            public long open(DataSpec dataSpec) throws IOException {
                return 0;
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public int read(byte[] bytes, int i, int i1) throws IOException {
                return 0;
            }
        };
    }
}
