package org.xvdr.recordings.model;

import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;

import java.util.Comparator;
import java.util.TreeSet;

public class SortedArrayObjectAdapter  extends ArrayObjectAdapter {

    private TreeSet<Object> mSortedItems;

    public SortedArrayObjectAdapter(Comparator comparator, PresenterSelector presenterSelector) {
        super(presenterSelector);
        mSortedItems = new TreeSet<Object>(comparator);
    }

    public SortedArrayObjectAdapter(Comparator comparator, Presenter presenter) {
        super(presenter);
        mSortedItems = new TreeSet<Object>(comparator);
    }

    public SortedArrayObjectAdapter(Comparator comparator) {
        super();
        mSortedItems = new TreeSet<Object>(comparator);
    }

    @Override
    public void add(Object item) {
        mSortedItems.add(item);
        super.add(mSortedItems.headSet(item).size(), item);
    }

    @Override
    public boolean remove(Object item) {
        mSortedItems.remove(item);
        return super.remove(item);
    }

    @Override
    public void clear() {
        mSortedItems.clear();
        super.clear();
    }
}
