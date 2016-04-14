package org.xvdr.recordings.util;

import android.content.Context;
import android.os.Build;

import org.xvdr.robotv.R;

public class Utils {

    private Utils() {
    }

    public static int dpToPx(int integerResource, Context ctx) {
        int dp = ctx.getResources().getInteger(integerResource);
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static int getColor(Context context, int id) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(id, null);
        }

        return context.getResources().getColor(id);
    }
}
