package org.xvdr.recordings.model;

import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;

import org.xvdr.recordings.presenter.MoviePresenter;
import org.xvdr.recordings.presenter.LatestCardPresenter;
import org.xvdr.robotv.R;

import java.util.Collection;
import java.util.Comparator;

public class MovieCollectionAdapter extends SortedArrayObjectAdapter {

    private MoviePresenter mCardPresenter;
    private LatestCardPresenter mLatestCardPresenter;
    private ArrayObjectAdapter mLatest;
    private ArrayObjectAdapter mTvShows;
    private Context mContext;

    static public Comparator<Movie> compareTimestamps = new Comparator<Movie>() {
        @Override
        public int compare(Movie lhs, Movie rhs) {
            return lhs.getTimeStamp() > rhs.getTimeStamp() ? -1 : 1;
        }
    };

    private static Comparator<ListRow> compareCategories = new Comparator<ListRow>() {
        @Override
        public int compare(ListRow lhs, ListRow rhs) {
            HeaderItem lhsHeader = lhs.getHeaderItem();
            HeaderItem rhsHeader = rhs.getHeaderItem();

            if(lhsHeader.getId() == 0) {
                return -1;
            }
            else if(rhsHeader.getId() == 0) {
                return 1;
            }
            else if(lhsHeader.getId() == 1) {
                return -1;
            }
            else if(rhsHeader.getId() == 1) {
                return 1;
            }
            else if(lhsHeader.getId() == 1000) {
                return 1;
            }
            else if(rhsHeader.getId() == 1000) {
                return -1;
            }

            return lhsHeader.getName().compareTo(rhsHeader.getName());
        }
    };

    public MovieCollectionAdapter(Context context) {
        super(compareCategories, new ListRowPresenter());
        mContext = context;
        mCardPresenter = new MoviePresenter();
        mLatestCardPresenter = new LatestCardPresenter();

        clear();
    }

    @Override
    public void clear() {
        super.clear();

        mLatest = getCategory(mContext.getString(R.string.latest_movies), true, mLatestCardPresenter); // 0
        mTvShows = getCategory(mContext.getString(R.string.tv_shows), true, mCardPresenter); // 1
    }

    private Movie movieExists(ArrayObjectAdapter adapter, Movie movie) {
        for(int i = 0; i < adapter.size(); i++) {
            Movie item = (Movie) adapter.get(i);

            if(item.getId().equals(movie.getId())) {
                return item;
            }
        }

        return null;
    }

    public ArrayObjectAdapter getCategory(Movie movie) {
        return getCategory(movie.getCategory());
    }

    private ArrayObjectAdapter getCategory(String category) {
        return getCategory(category, false);
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew) {
        return getCategory(category, addNew, mCardPresenter);
    }

    private ListRow findRow(String category) {
        ListRow listrow;

        for(int i = 0; i < size(); i++) {
            listrow = (ListRow)get(i);

            if(listrow.getHeaderItem().getName().equalsIgnoreCase(category)) {
                return listrow;
            }
        }

        return null;
    }

    public ListRow findRow(long id) {
        ListRow listrow;

        for(int i = 0; i < size(); i++) {
            listrow = (ListRow)get(i);

            if(listrow.getId() == id) {
                return listrow;
            }
        }

        return null;
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew, Presenter presenter) {
        if(TextUtils.isEmpty(category)) {
            return null;
        }

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

        HeaderItem header = new HeaderItem(size(), category);
        ArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(compareTimestamps, presenter);

        listrow = new ListRow(header, listRowAdapter);
        listrow.setId(size());

        add(listrow);

        return listRowAdapter;
    }

    public void add(Movie movie) {
        // add into "latest" category
        Movie item = movieExists(mLatest, movie);

        if(item != null) {
            item.setArtwork(movie);
        }
        else {
            mLatest.add(movie);
        }

        if(movie.isSeries()) {
            addSeriesEpisode(movie);
            return;
        }

        addMovie(movie);
    }

    private void addMovie(Movie movie) {
        ArrayObjectAdapter row = getCategory(movie.getCategory(), true);

        if(row == null) {
            return;
        }

        Movie item = movieExists(row, movie);

        if(item != null) {
            item.setArtwork(movie);
        }
        else {
            row.add(movie);
        }
    }

    private void addSeriesEpisode(Movie episode) {
        // check if series item already exists
        for(int i = 0; i < mTvShows.size(); i++) {
            Movie m = (Movie) mTvShows.get(i);
            if(m.getTitle().equals(episode.getTitle())) {
                m.setEpisodeCount(m.getEpisodeCount() + 1);
                return;
            }
        }

        // create a new item for this series
        Movie series = new Movie();
        series.setTitle(episode.getTitle());
        series.setContent(0x15);
        series.setCardImageUrl(episode.getCardImageUrl());
        series.setBackgroundImageUrl(episode.getBackgroundImageUrl());
        series.setEpisodeCount(1);
        series.setSeriesHeader();

        mTvShows.add(series);
        return;
    }

    public void remove(Movie movie) {
        ArrayObjectAdapter row = getCategory(movie.getCategory(), true);

        if(row == null) {
            return;
        }

        row.remove(movie);
    }

    public void cleanup() {
        ListRow tvShowsRow = findRow("TV Shows");

        if(tvShowsRow != null && tvShowsRow.getAdapter().size() == 0) {
            remove(tvShowsRow);
        }
    }

    public void addAllMovies(Collection<Movie> movieCollection) {
        for(Movie movie : movieCollection) {
            add(movie);
        }
    }
}
