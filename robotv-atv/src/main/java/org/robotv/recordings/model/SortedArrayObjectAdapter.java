package org.robotv.recordings.model;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.Presenter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

public class SortedArrayObjectAdapter  extends ArrayObjectAdapter {

    private final TreeSet mSortedItems;

    public SortedArrayObjectAdapter(Comparator comparator, Presenter presenter) {
        super(presenter);
        mSortedItems = new TreeSet(comparator);
    }

    @Override
    public void add(Object item) {
        mSortedItems.add(item);
        int index = mSortedItems.headSet(item).size();

        if(index > size()) {
            super.add(item);
            return;
        }

        super.add(index, item);
    }

    public void append(Object item) {
        super.add(item);
    }

    public void addAll(ArrayList<?> items) {

        for(Object item : items) {
            add(item);
        }
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
