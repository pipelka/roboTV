package org.xvdr.player;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;

class TrickPlayController {

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
            postTick();
        }

        started = true;
    }

    private void tick() {
        long diff = (System.currentTimeMillis() - startTime) * playbackSpeed;
        long seekPosition = startPosition + diff;

        // clamp to end position
        seekPosition = Math.min(seekPosition, position.getEndPosition());

        long timeUs = position.timeUsFromPosition(seekPosition);
        player.seekTo(timeUs / 1000);
    }

    void stop() {
        if(!started) {
            return;
        }

        tick();
        reset();

        player.setPlayWhenReady(true);
    }

    void reset() {
        handler.removeCallbacks(doTick);
        position.setTrickPlayMode(false);
        playbackSpeed = 1;
        started = false;
    }

    void postTick() {
        handler.post(doTick);
    }

    boolean activated() {
        return started;
    }
}
