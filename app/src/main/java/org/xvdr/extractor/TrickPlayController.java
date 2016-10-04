package org.xvdr.extractor;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;

class TrickPlayController {

    final private long TICK_TIME = 1000;

    final private PositionReference position;
    final private ExoPlayer player;
    final private Handler handler;

    private long startTime = 0;
    private long startPosition = 0;
    private long playbackSpeed = 1;
    private boolean started = false;

    private Runnable doTick = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(doTick, TICK_TIME);
        }
    };

    TrickPlayController(Handler handler, PositionReference position, ExoPlayer player) {
        this.position = position;
        this.player = player;
        this.handler = handler;
    }

    void start(float speed) {
        if(speed == 1.0) {
            stop();
            return;
        }

        player.setPlayWhenReady(false);
        startTime = System.currentTimeMillis();
        startPosition = position.positionFromTimeUs(player.getCurrentPosition() * 1000);
        playbackSpeed = (int) speed;
        position.setTrickPlayMode(true);

        if(!started) {
            handler.post(doTick);
        }
        started = true;
    }

    private void tick() {
        long diff = (System.currentTimeMillis() - startTime) * playbackSpeed;
        long seekPosition = startPosition + diff;

        long timeUs = position.timeUsFromPosition(seekPosition);
        player.seekTo(timeUs / 1000);
    }

    void stop() {
        if(!started) {
            return;
        }

        handler.removeCallbacks(doTick);
        position.setTrickPlayMode(false);

        tick();
        playbackSpeed = 1;
        started = false;
        player.setPlayWhenReady(true);
    }
}
