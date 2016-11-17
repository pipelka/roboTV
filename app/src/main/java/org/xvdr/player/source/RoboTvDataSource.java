package org.xvdr.player.source;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import org.xvdr.jniwrap.Packet;
import org.xvdr.player.PositionReference;
import org.xvdr.robotv.client.Connection;

import java.io.IOException;
import java.io.InterruptedIOException;

class RoboTvDataSource implements DataSource {

    private final static String TAG = RoboTvDataSource.class.getName();

    public interface Listener {

        void onOpenStreamError(int status);

    }

    private String language;
    private Uri uri;
    private boolean streaming = false;

    final private Connection connection;
    final private Packet request;
    final private Packet response;
    final private PositionReference position;
    final private Listener listener;

    RoboTvDataSource(PositionReference position, Connection connection, String language, Listener listener) {
        this.language = language;
        this.connection = connection;
        this.position = position;
        this.listener = listener;

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

        // check if we should seek
        long seekPosition = dataSpec.position;
        if(streaming && (lastUri != null && uri.equals(lastUri))) {

            if(seekPosition != 0) {
                Log.d(TAG, "seek to position: " + seekPosition);
                response.clear();
                connection.seek(seekPosition);
            }

            return C.LENGTH_UNSET;
        }

        response.clear();

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
                streaming = openLiveTv(uri);
                break;

            case "recording":
                streaming = openRecording(uri);
                break;

            default:
                throw new IOException("unsupported stream type: " + type + ")");
        }

        return C.LENGTH_UNSET;
    }

    private boolean openLiveTv(Uri uri) throws IOException {
        String path = uri.getPath();
        int channelUid = Integer.parseInt(path.substring(1));

        int status = connection.openStream(channelUid, language, false, 50);

        if(status == Connection.STATUS_SUCCESS) {
            Log.d(TAG, "live stream opened");
            return true;
        }

        Log.e(TAG, "unable to open live stream (status: " + status + ")");

        if(listener != null) {
            listener.onOpenStreamError(status);
        }

        return false;
    }

    private boolean openRecording(Uri uri) throws IOException {
        String recordingId = uri.getPath().substring(1);
        long position = Long.parseLong(uri.getQueryParameter("position"));

        int status = connection.openRecording(recordingId, position);

        if(status == Connection.STATUS_SUCCESS) {
            Log.d(TAG, "recording opened");
            return true;
        }

        Log.e(TAG, "unable to open recording (status: " + status + ")");

        if(listener != null) {
            listener.onOpenStreamError(status);
        }

        return false;
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
        while(response.eop()) {

            if(Thread.interrupted()) {
                throw new InterruptedIOException();
            }

            request.createUid();
            request.putU8(position.getTrickPlayMode() ? (short)1 : (short)0);

            if(!connection.transmitMessage(request, response)) {
                throw new IOException("failed to request packet from server");
            }

            // empty packet ??
            try {
                if(response.eop()) {
                    Thread.sleep(100);
                    continue;
                }
            }
            catch(InterruptedException e) {
                throw new InterruptedIOException();
            }

            // start position of stream (wallclock time) - monotonic increasing
            long pos = response.getS64();
            if(pos > position.getStartPosition()) {
                position.setStartPosition(pos);
            }

            // end position of stream - if any (wallclock time) - monotonic increasing
            pos = response.getS64();
            if(pos == 0) {
                pos = System.currentTimeMillis();
            }

            if(pos > position.getEndPosition()) {
                position.setEndPosition(pos);
            }
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

    public void disconnect() throws IOException {
        streaming = false;
        close();
    }
}
