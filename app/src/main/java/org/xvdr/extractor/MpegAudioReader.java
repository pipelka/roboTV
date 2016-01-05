package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.MpegAudioHeader;

import org.xvdr.audio.MpegAudioDecoder;
import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR MPEG Audio stream.
 */
final class MpegAudioReader extends StreamReader {

    private static final String TAG = "MpegAudioReader";
    boolean hasOutputFormat = false;

    MpegAudioDecoder mDecoder;

    public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream) {
        this(output, stream, false);
    }

    public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream, boolean useHwDecoder) {
        super(output, stream);

        if(!useHwDecoder) {
            mDecoder = new MpegAudioDecoder();
        }
    }

    @Override
    public void consume(byte[] data, long pesTimeUs, boolean isKeyframe) {
        if(mDecoder == null) {
            consumeHw(data, pesTimeUs, isKeyframe);
            return;
        }

        int length = mDecoder.decode(data, 0, data.length);

        if (length == 0) {
            return;
        }

        byte[] audioChunk = new byte[length];

        if (!mDecoder.read(audioChunk, 0, audioChunk.length)) {
            Log.e(TAG, "failed to read audio chunk");
            return;
        }

        if (!hasOutputFormat) {
            MediaFormat format = MediaFormat.createAudioFormat(
                    Integer.toString(stream.physicalId), // < trackId
                    MimeTypes.AUDIO_RAW,
                    MediaFormat.NO_VALUE,
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

    private void consumeHw(byte[] data, long pesTimeUs, boolean isKeyframe) {
        if (!hasOutputFormat) {
            int  header = ((data[0] << 24) | (data[1] << 16) | (data[2] <<  8) | data[3]);
            MpegAudioHeader mpegAudioHeader = new MpegAudioHeader();

            if(MpegAudioHeader.populateHeader(header, mpegAudioHeader)) {
                MediaFormat mediaFormat = MediaFormat.createAudioFormat(
                        Integer.toString(stream.physicalId), // < trackId
                        mpegAudioHeader.mimeType,
                        mpegAudioHeader.bitrate,
                        MpegAudioHeader.MAX_FRAME_SIZE_BYTES,
                        C.UNKNOWN_TIME_US,
                        mpegAudioHeader.channels,
                        mpegAudioHeader.sampleRate,
                        null,
                        stream.language);

                output.format(mediaFormat);
                hasOutputFormat = true;
            }
        }

        output.sampleData(data, data.length, pesTimeUs, C.SAMPLE_FLAG_SYNC);
    }

}