package org.xvdr.recordings.presenter;

import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Action;

public class ColorAction extends Action {
    private int color;

    public ColorAction(long id) {
        super(id);
    }

    public ColorAction(long id, CharSequence label) {
        super(id, label);
    }

    public ColorAction(long id, CharSequence label1, CharSequence label2) {
        super(id, label1, label2);
    }

    public ColorAction(long id, CharSequence label1, CharSequence label2, Drawable icon) {
        super(id, label1, label2, icon);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
