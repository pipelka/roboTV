package org.xvdr.robotv.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

import java.io.IOException;

public class NotificationHandler {

    private final static String TAG = "NotificationHandler";

    private Context mContext;
    private Handler mHandler;

    public NotificationHandler(Context context) {
        mContext = context;
        mHandler = new Handler();
    }

    public void notify(String message) {
        notify(
            message,
            mContext.getResources().getString(R.string.toast_information),
            R.drawable.ic_info_outline_white_48dp);
    }

    public void error(String message) {
        notify(
            message,
            mContext.getResources().getString(R.string.toast_error),
            R.drawable.ic_error_outline_white_48dp);
    }


    public void notify(final String message, final String title, final String imageUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "loading bitmap with picasso");
                Bitmap bitmap = null;

                try {
                    bitmap = Picasso.with(mContext)
                             .load(imageUrl)
                             .resize(Utils.dpToPx(R.integer.artwork_background_width_small, mContext),
                                     Utils.dpToPx(R.integer.artwork_background_height_small, mContext))
                             .centerCrop()
                             .get();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }

                if(bitmap != null) {
                    NotificationHandler.this.notify(message, title, new BitmapDrawable(mContext.getResources(), bitmap));
                }
                else {
                    NotificationHandler.this.notify(message, title, R.drawable.ic_movie_white_48dp);
                }
            }
        }).start();
    }

    public void notify(final String message, final String title, final Drawable d) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "notify: " + message + " / " + title);
                final Toast toast = new Toast(mContext);

                final DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
                metrics.density = 2.0f;
                metrics.scaledDensity = 2.0f;

                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View view = inflater.inflate(R.layout.layout_toast, null);

                TextView titleView = (TextView) view.findViewById(R.id.title);
                titleView.setText(title);

                TextView messageView = (TextView) view.findViewById(R.id.message);
                messageView.setText(message);

                ImageView imageView = (ImageView) view.findViewById(R.id.icon);
                imageView.setImageDrawable(d);

                toast.setView(view);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setGravity(Gravity.RIGHT | Gravity.BOTTOM, 0, 0);

                toast.show();
            }
        });
    }

    public void notify(String message, String title, final int icon) {
        notify(message, title, mContext.getDrawable(icon));
    }

}
