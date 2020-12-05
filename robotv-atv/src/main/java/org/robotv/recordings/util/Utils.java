package org.robotv.recordings.util;

import android.content.Context;
import android.os.Build;

public class Utils {

    private Utils() {
    }

    public static int dp(int integerResource, Context ctx) {
        return ctx.getResources().getInteger(integerResource);
    }

    public static int dpToPx(int integerResource, Context ctx) {
        int dp = ctx.getResources().getInteger(integerResource);
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static int getColor(Context context, int id) {
        return context.getResources().getColor(id, null);
    }
}
