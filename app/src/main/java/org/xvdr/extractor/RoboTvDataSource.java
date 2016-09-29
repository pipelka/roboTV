package org.xvdr.extractor;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.client.Connection;

import java.io.IOException;

class RoboTvDataSource implements DataSource {

    private final static String TAG = RoboTvDataSource.class.getName();

    private String language;
    private Uri uri;
    private boolean streaming = false;

    final private Connection connection;
    final private Packet request;
    final private Packet response;
    final private PositionReference position;

    RoboTvDataSource(PositionReference position, Connection connection, String language) {
        this.language = language;
        this.connection = connection;
        this.position = position;

        response = new Packet();
        request = connection.CreatePacket(Connection.XVDR_CHANNELSTREAM_REQUEST, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
    }

    @Override
    synchronized public long open(DataSpec dataSpec) throws IOException {
        // the uri should be something like:
        // robotv://type/channeluid (e.g. robotv://livetv/5475634 )

        Uri lastUri = getUri();
        uri = dataSpec.uri;

        Log.d(TAG, "open: " + uri.toString());

        response.clear();

        // check if we should seek
        if(streaming && (lastUri != null && uri.equals(lastUri))) {
            long seekPosition = dataSpec.position;
            Log.d(TAG, "start at position: " + seekPosition);
            connection.seek(seekPosition);
            return C.LENGTH_UNSET;
        }

        if(!uri.getScheme().equals("robotv")) {
            throw new IOException("unable to open stream: " + getUri().toString() + ", uri must start with robotv://");
        }

        if(!connection.isOpen()) {
            throw new IOException();
        }

        Log.d(TAG, "start streaming: " + uri.toString());

        String type = uri.getHost();
        switch(type) {
            case "livetv":
                openLiveTv(uri);
                break;
            case "recording":
                openRecording(uri);
                break;
            default:
                throw new IOException("unsupported stream type: " + type + ")");
        }

        streaming = true;
        return C.LENGTH_UNSET;
    }

    private void openLiveTv(Uri uri) throws IOException {
        String path = uri.getPath();
        int channelUid = Integer.parseInt(path.substring(1));

        int status = connection.openStream(channelUid, language, false, 50);

        if (status != Connection.STATUS_SUCCESS) {
            throw new IOException("unable to start streaming (status " + status + ")");
        }

        Log.d(TAG, "live stream opened");
    }

    private void openRecording(Uri uri) throws IOException {
        String recordingId = uri.getPath().substring(1);

        int status = connection.openRecording(recordingId);

        if (status != Connection.STATUS_SUCCESS) {
            throw new IOException("unable to open recording (status " + status + ")");
        }

        Log.d(TAG, "recording opened");
    }

    public void release() {
        Log.d(TAG, "release");
        connection.closeStream();
        streaming = false;
        uri = null;
    }

    @Override
    synchronized public int read(byte[] buffer, int offset, int readLength) throws IOException {
        // request a new packet if we have completely consumed the old one
        if(response.eop()) {
            request.createUid();
            request.putU8(position.getTrickPlayMode() ? (short)1 : (short)0);

            if(!connection.transmitMessage(request, response)) {
                throw new IOException("failed to request packet from server");
            }

            // empty packet ??
            if(response.eop()) {
                return 0;
            }

            // start position of stream (wallclock time)
            position.setStartPosition(response.getS64());

            // end position of stream - if any (wallclock time)
            long endPosition = response.getS64();
            position.setEndPosition(endPosition == 0 ? C.TIME_UNSET : endPosition);
        }

        readLength = Math.min(readLength, response.remaining());
        response.readBuffer(buffer, offset, readLength);

        return readLength;
    }

    @Override
    public Uri getUri() {
        if(!streaming) {
            return null;
        }

        return uri;
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "close");
        response.clear();
    }
}
