package org.robotv.recordings.presenter;

import android.graphics.drawable.Drawable;
import androidx.leanback.widget.Action;

public class ColorAction extends Action {
    private int color;

    public ColorAction(long id, CharSequence label1, CharSequence label2, Drawable icon) {
        super(id, label1, label2, icon);
    }

    public ColorAction setColor(int color) {
        this.color = color;
        return this;
    }

    public int getColor() {
        return color;
    }
}
