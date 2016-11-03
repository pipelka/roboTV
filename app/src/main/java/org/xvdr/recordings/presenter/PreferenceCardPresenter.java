package org.xvdr.recordings.presenter;

import android.graphics.Color;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

public class PreferenceCardPresenter extends Presenter {

    static public class Style {
        private int iconResource;
        private int id;

        public Style(int id, String text, int iconResource) {
            this.id = id;
            this.iconResource = iconResource;
        }

        public int getId() {
            return id;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageView view = new ImageView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(
                                 parent.getResources().getInteger(R.integer.preference_square_size),
                                 parent.getResources().getInteger(R.integer.preference_square_size)));

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setPadding(50, 50, 50, 50);

        view.setBackgroundColor(Utils.getColor(parent.getContext(), R.color.default_background));

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Style style = (Style) item;
        ImageView view = (ImageView) viewHolder.view;

        view.setImageDrawable(view.getResources().getDrawable(style.iconResource, null));
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
