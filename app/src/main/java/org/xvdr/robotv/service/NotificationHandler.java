package org.xvdr.robotv.service;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xvdr.robotv.R;

public class NotificationHandler {

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



    public void notify(final String message, final String title, final int icon) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final Toast toast = new Toast(mContext);
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View view = inflater.inflate(R.layout.layout_toast, null);

                TextView titleView = (TextView) view.findViewById(R.id.title);
                titleView.setText(title);

                TextView messageView = (TextView) view.findViewById(R.id.message);
                messageView.setText(message);

                ImageView imageView = (ImageView) view.findViewById(R.id.icon);
                imageView.setImageResource(icon);

                toast.setView(view);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setGravity(Gravity.RIGHT | Gravity.BOTTOM, 0, 0);

                toast.show();
            }
        });
    }

}
