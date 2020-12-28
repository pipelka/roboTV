package org.robotv.channelstress;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robotv.client.Channels;
import org.robotv.client.Connection;
import org.robotv.client.model.Channel;
import org.robotv.msgexchange.Packet;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ServerTest {

    static class TestConnection extends Connection {

        private static final String TAG = "TestConnection";

        public TestConnection(String sessionName) {
            super(sessionName);
            setTimeout(5000);
        }

        public int stream(long channelUid) {
            Packet req = CreatePacket(Connection.CHANNELSTREAM_OPEN, Connection.CHANNEL_REQUEST_RESPONSE);
            req.putU32(channelUid);
            req.putS32(50); // priority 50
            req.putU8((short) 0); // start with IFrame
            req.putU8((short)1); // raw PTS values
            req.putString("deu");

            Packet resp = transmitMessage(req);

            if(resp == null) {
                return -2;
            }

            return (int) resp.getU32();
        }

        public void startChannelSwitchTest() {
            Channels channels = new Channels();
            channels.load(this, "deu");

            for(Channel entry : channels) {

                Log.i(TAG, "channel: " + entry.getName());
                int status = stream(entry.getUid());
                Log.i(TAG, "switch status: " + status);
                assertTrue(status != -2);

                int durationMs = ThreadLocalRandom.current().nextInt(100, 2000 + 1);
                try {
                    TimeUnit.MILLISECONDS.sleep(durationMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                Packet req = CreatePacket(Connection.CHANNELSTREAM_REQUEST, Connection.CHANNEL_REQUEST_RESPONSE);
                Packet resp = transmitMessage(req);

                assertNotNull(resp);
            }
        }
    }

    @Test
    public void singleConnectionChannelSwitchTest() {
        TestConnection connection = new TestConnection("roboTV server test");
        assertTrue(connection.open("192.168.16.153"));

        connection.startChannelSwitchTest();
    }

    //@Test
    public void testChannelSwitchStress() {
        Thread t1 = new Thread(this::singleConnectionChannelSwitchTest);
        Thread t2 = new Thread(this::singleConnectionChannelSwitchTest);
        Thread t3 = new Thread(this::singleConnectionChannelSwitchTest);

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        }
        catch (InterruptedException e) {
            fail();
            e.printStackTrace();
        }
    }
}
