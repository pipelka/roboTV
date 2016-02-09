package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.audio.AC3Decoder;
import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends StreamReader {

    private static final String TAG = "Ac3Reader";
    private boolean ac3PassThrough;
    boolean hasOutputFormat = false;

    AC3Decoder mDecoder;

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream) {
        this(output, stream, false);
    }

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, boolean ac3PassThrough) {
        this(output, stream, ac3PassThrough, Player.CHANNELS_SURROUND);
    }

	public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, boolean ac3PassThrough, int channelConfiguration) {
		super(output, stream);
        this.ac3PassThrough = ac3PassThrough;

        if(ac3PassThrough) {
            output.format(MediaFormat.createAudioFormat(
                    Integer.toString(stream.physicalId),
                    MimeTypes.AUDIO_AC3,
                    stream.bitRate,
                    MediaFormat.NO_VALUE,
                    C.UNKNOWN_TIME_US,
                    stream.channels,
                    stream.sampleRate,
                    null,
                    stream.language));
            return;
        }

        int decoderMode = AC3Decoder.LayoutDolby;

        if(channelConfiguration == Player.CHANNELS_DIGITAL51) {
            decoderMode = AC3Decoder.Layout51;
        }
        else if(channelConfiguration == Player.CHANNELS_STEREO) {
            decoderMode = AC3Decoder.LayoutStereo;
        }

        mDecoder = new AC3Decoder(decoderMode);
	}

	@Override
	public void consume(byte[] data, long pesTimeUs) {
        if(ac3PassThrough) {
            output.sampleData(data, data.length, pesTimeUs, C.SAMPLE_FLAG_SYNC);
            return;
        }

        int length = mDecoder.decode(data, 0, data.length);

        if(length == 0) {
            Log.e(TAG, "Unable to decode frame data");
            return;
        }

        byte[] audioChunk = new byte[length];

        if(!mDecoder.read(audioChunk, 0, audioChunk.length)) {
            Log.e(TAG, "failed to read audio chunk");
            return;
        }

        if(!hasOutputFormat) {
            Log.i(TAG, "channels: " + mDecoder.getChannels());
            MediaFormat format = MediaFormat.createAudioFormat(
                    Integer.toString(stream.physicalId), // < trackId
                    MimeTypes.AUDIO_RAW,
                    mDecoder.getBitRate(),
                    MediaFormat.NO_VALUE,
                    C.UNKNOWN_TIME_US,
                    mDecoder.getChannels(),
                    mDecoder.getSampleRate(),
                    null,
                    stream.language);
            output.format(format);
            hasOutputFormat = true;
        }

        output.sampleData(audioChunk, audioChunk.length, pesTimeUs, C.SAMPLE_FLAG_SYNC);
	}

}
