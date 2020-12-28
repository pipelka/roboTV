package org.robotv.timer;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robotv.client.Connection;
import org.robotv.client.TimerController;
import org.robotv.client.model.Timer;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TimerTest {

    static final private String TAG = "TimerTest";

    Connection connection;
    TimerController controller;

    public TimerTest() {
        connection = new Connection("TimerTest", "deu");
        assertTrue(connection.open("192.168.16.153"));

        controller = new TimerController(connection);
    }

    @Test
    public void testGetTimers() {
        controller.loadTimers(timers -> {
            assertNotNull(timers);

            for(Timer timer: timers) {
                Log.i(TAG, "TIMER");
                Log.i(TAG, "title:     " + timer.getTitle());
                Log.i(TAG, "subtitle:  " + timer.getShortText());
                Log.i(TAG, "channel:   " + timer.getChannelUid());
                Log.i(TAG, "starttime: " + timer.getTimestamp());
                Log.i(TAG, "logo:      " + timer.getLogoUrl());
                Log.i(TAG, "poster:    " + timer.getPosterUrl());
                Log.i(TAG, "searchId:  " + timer.getSearchTimerId());
            }

            assertTrue(timers.size() >= 0);
        });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetSearchTimers() {
        controller.loadSearchTimers(timers -> {
            assertNotNull(timers);

            for(Timer timer: timers) {
                Log.i(TAG, "TIMER");
                Log.i(TAG, "searchId:  " + timer.getSearchTimerId());
                Log.i(TAG, "title:     " + timer.getTitle());
                Log.i(TAG, "channel:   " + timer.getChannelUid());
                Log.i(TAG, "logo:      " + timer.getLogoUrl());
                Log.i(TAG, "poster:    " + timer.getPosterUrl());
            }

            assertTrue(timers.size() >= 0);
        });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
