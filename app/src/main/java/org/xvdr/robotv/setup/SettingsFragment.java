package org.xvdr.robotv.setup;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xvdr.robotv.R;

public class SettingsFragment extends BrowseFragment {
    private static final String TAG = "SettingsFragment";

    private static final int GRID_ITEM_WIDTH = 300;
    private static final int GRID_ITEM_HEIGHT = 300;

    private ArrayObjectAdapter mRowsAdapter;

    class SettingsItem {
        SettingsItem(int id, String text, int resource) {
            this.text = text;
            this.id = id;
            this.resource = resource;
        }

        int id;
        String text;
        int resource;
    }
    // Container Activity must implement this interface
    public interface SettingsClickedListener {
        public void onSettingsClicked();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupUIElements();

        loadRows();

        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void loadRows() {

        String inputId = getActivity().getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);

        GridItemPresenter mGridPresenter = new GridItemPresenter();

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        HeaderItem gridHeader = new HeaderItem(0, "Settings", null);

        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new SettingsItem(1, "Server", android.R.drawable.ic_menu_preferences));
        gridRowAdapter.add(new SettingsItem(2, "Streaming", android.R.drawable.ic_media_play));
        gridRowAdapter.add(new SettingsItem(3, "Channel filters", android.R.drawable.ic_menu_search));
        gridRowAdapter.add(new SettingsItem(4, "Timeshift", android.R.drawable.ic_media_pause));

        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        setAdapter(mRowsAdapter);
    }

    private void setupUIElements() {
        setTitle(getString(R.string.robotv_settings));
        setHeadersState(HEADERS_DISABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof SettingsItem) {
                SettingsItem settingsItem = (SettingsItem) item;
                Toast.makeText(getActivity(), settingsItem.text, Toast.LENGTH_SHORT).show();
            }
        }
    }


    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.settings_item, null);
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ImageView icon = (ImageView) viewHolder.view.findViewById(R.id.icon);
            TextView text = (TextView) viewHolder.view.findViewById(R.id.text);

            SettingsItem settingsItem = (SettingsItem) item;
            text.setText(settingsItem.text);

            if(settingsItem.resource != 0) {
                icon.setImageResource(settingsItem.resource);
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}
