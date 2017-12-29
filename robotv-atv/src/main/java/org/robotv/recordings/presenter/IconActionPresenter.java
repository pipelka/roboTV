package org.robotv.recordings.presenter;

import android.content.res.Resources;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;

import org.robotv.recordings.model.IconAction;
import org.robotv.robotv.R;

public class IconActionPresenter extends Presenter {

    private int width = 250;
    private int height = 220;

    private int iconWidth = 48;
    private int iconHeight = 48;

    public IconActionPresenter() {
    }

    public IconActionPresenter(int width, int height) {
        this.width = width;
        this.height = height;
    }

    static public class ViewHolder extends Presenter.ViewHolder {
        private ImageCardView cardView;

        public ViewHolder(View view) {
            super(view);
            cardView = (ImageCardView) view;
        }

        ImageCardView getCardView() {
            return cardView;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(parent.getContext(), R.style.IconActionCardTheme);
        ImageCardView cardView = new ImageCardView(wrapper);

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);

        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (item instanceof IconAction) {
            IconAction action = (IconAction) item;
            ViewHolder vh = (ViewHolder) viewHolder;
            ImageCardView cardView = vh.getCardView();
            Resources resources = cardView.getResources();

            cardView.setTitleText(action.getText());
            cardView.setMainImage(resources.getDrawable(action.getResourceId(), null));
            cardView.setMainImageDimensions(width, height);

            int paddingWidth = (width - iconWidth);
            int paddingHeight = (height - iconHeight);

            cardView.getMainImageView().setPadding(
                    paddingWidth,
                    paddingHeight,
                    paddingWidth,
                    paddingHeight);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

}
