package org.robotv.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.app.ProgressBarManager;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ObjectAdapter;

public class SearchProgressFragment extends SearchSupportFragment implements SearchSupportFragment.SearchResultProvider {

    private final ProgressBarManager progress;

    public SearchProgressFragment() {
        this(500);
    }

    public SearchProgressFragment(int initialDelay) {
        progress = new ProgressBarManager();
        progress.setInitialDelay(initialDelay);
    }

    protected void showProgress(boolean show) {
        if(show) {
            progress.show();
            progress.enableProgressBar();
            return;
        }

        progress.disableProgressBar();
        progress.hide();
    }

    protected void setInitialDelay(long initialDelay) {
        progress.setInitialDelay(initialDelay);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        progress.setRootView((ViewGroup) view);

        return view;
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return null;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(TextUtils.isEmpty(query)) {
            return false;
        }

        showProgress(true);
        return true;
    }
}
