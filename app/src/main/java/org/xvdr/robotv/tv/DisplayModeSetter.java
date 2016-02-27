package org.xvdr.robotv.tv;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class DisplayModeSetter {

    private static final String TAG = "DisplayModeSetter";

    private WindowManager.LayoutParams mParams = null;
    private View mWindow = null;
    private Context mContext;

    public DisplayModeSetter(Context context) {
        mContext = context;

        mParams = new WindowManager.LayoutParams(0, 0, 0, 0, 2006, 40, -3);
        mParams.gravity = 17;
        mParams.setTitle(TAG);
    }

    private WindowManager getWindowManager() {
        return (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    private void setMode() {
        if(mWindow == null) {
            mWindow = new View(mContext);
            mWindow.setBackgroundColor(0);

            getWindowManager().addView(mWindow, mParams);
        }
        else {
            getWindowManager().updateViewLayout(mWindow, mParams);
        }
    }

    public void release() {
        if(mWindow != null) {
            getWindowManager().removeView(this.mWindow);
        }
    }

    public void setRefreshRate(float refreshRate) {

        if(refreshRate < 0.0f) {
            release();
            return;
        }

        Log.d(TAG, "refreshrate: " + refreshRate);

        float[] rates = getWindowManager().getDefaultDisplay().getSupportedRefreshRates();

        for(float rate : rates) {
            if(Math.abs(refreshRate - rate) < 0.1f) {
                refreshRate = rate;
                mParams.preferredRefreshRate = refreshRate;
                Log.d(TAG, "setting refreshrate to " + refreshRate + " Hz");
                setMode();
                return;
            }

        }

        Log.d(TAG, "failed to set refreshrate");
    }
}
