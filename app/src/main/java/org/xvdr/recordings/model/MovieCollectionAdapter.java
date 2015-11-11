package org.xvdr.recordings.model;

import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.util.ArrayMap;

import org.xvdr.recordings.presenter.CardPresenter;
import org.xvdr.recordings.presenter.LatestCardPresenter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MovieCollectionAdapter extends ArrayObjectAdapter {

    private CardPresenter mCardPresenter;
    private LatestCardPresenter mLatestCardPresenter;
    private ArrayMap<String, ListRow> mSeriesMap;
    private List<Movie> mList;
    private ArrayObjectAdapter mLatest;

    private Comparator<Movie> compareTimestamps = new Comparator<Movie>() {
        @Override
        public int compare(Movie lhs, Movie rhs) {
            return lhs.getTimeStamp() > rhs.getTimeStamp() ? -1 : 1;
        }
    };


    public MovieCollectionAdapter() {
        super(new ListRowPresenter());
        mCardPresenter = new CardPresenter();
        mLatestCardPresenter = new LatestCardPresenter();

        mSeriesMap = new ArrayMap<>();
        mList = new ArrayList<>();

        mLatest = getCategory("Latest", true, mLatestCardPresenter);
    }

    private ArrayObjectAdapter getCategory(String category) {
        return getCategory(category, false);
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew) {
        return getCategory(category, addNew, mCardPresenter);
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew, Presenter presenter) {
        ListRow listrow;

        for(int i = 0; i < size(); i++) {
            listrow = (ListRow)get(i);
            if(listrow.getHeaderItem().getName().equalsIgnoreCase(category)) {
                return (ArrayObjectAdapter) listrow.getAdapter();
            }
        }

        if(!addNew) {
            return null;
        }

        HeaderItem header = new HeaderItem(size() - 1, category);
        ArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(compareTimestamps, presenter);

        listrow = new ListRow(header, listRowAdapter);
        add(listrow);

        return listRowAdapter;
    }

    private ObjectAdapter getSeries(String title, boolean addNew) {
        ArrayObjectAdapter row = (ArrayObjectAdapter) getCategory("Serien", true);
        if(row == null) {
            return null;
        }

        // get adapter of series
        ListRow seriesRow = mSeriesMap.get(title);

        if(seriesRow != null) {
            return seriesRow.getAdapter();
        }

        if(!addNew) {
            return null;
        }

        // create a new one for this series
        Movie series = new Movie();
        series.setTitle(title);
        series.setSeries(true);

        row.add(series);

        // create a new adapter for this series
        ArrayObjectAdapter seriesRowAdapter = new SortedArrayObjectAdapter(compareTimestamps, mCardPresenter);
        mSeriesMap.put(title, new ListRow(null, seriesRowAdapter));

        return seriesRowAdapter;
    }

    public ArrayObjectAdapter add(Movie movie) {
        mList.add(movie);

        // add into "latest" category
        mLatest.add(movie);

        if(movie.isSeries()) {
            return addSeriesEpisode(movie);
        }

        return addMovie(movie);
    }

    protected ArrayObjectAdapter addMovie(Movie movie) {
        ArrayObjectAdapter row = (ArrayObjectAdapter) getCategory(movie.getCategory(), true);
        if(row == null) {
            return null;
        }

        row.add(movie);
        return row;
    }

    protected ArrayObjectAdapter addSeriesEpisode(Movie episode) {
        ArrayObjectAdapter seriesRow = (ArrayObjectAdapter) getSeries(episode.getCategory(), true);
        if(seriesRow == null) {
            return null;
        }

        seriesRow.add(episode);
        return seriesRow;
    }

    public void remove(Movie movie) {
        ArrayObjectAdapter row = (ArrayObjectAdapter) getCategory(movie.getCategory(), true);
        if(row == null) {
            return;
        }

        row.remove(movie);
    }
}
