package org.robotv.epgsearch;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robotv.client.Connection;
import org.robotv.client.PacketAdapter;
import org.robotv.client.model.Event;
import org.robotv.msgexchange.Packet;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EpgSearchTest {

    static final private String TAG = "EpgSearchTest";

    Connection mConnection;

    public EpgSearchTest() {
        mConnection = new Connection("epgSearchTest", "deu");
        assertTrue(mConnection.open("192.168.16.153"));
    }

    @Test
    public void testEpgSearch() {
        Packet req = mConnection.CreatePacket(
                Connection.EPG_SEARCH,
                Connection.CHANNEL_REQUEST_RESPONSE);

        req.putString("wetter");

        Packet resp = mConnection.transmitMessage(req);
        assertNotNull(resp);

        resp.uncompress();

        while(!resp.eop()) {
            Event event = PacketAdapter.toEvent(resp);
            String channelName = resp.getString();
            int channelUid = (int) resp.getU32();
            int channelNumber = (int) resp.getU32();

            Log.i(TAG, "EVENT");
            Log.i(TAG, "title:     " + event.getTitle());
            Log.i(TAG, "subtitle:  " + event.getShortText());
            Log.i(TAG, "channel:   " + channelName);
            Log.i(TAG, "starttime: " + event.getTimestamp().toString());
        }
    }
}
