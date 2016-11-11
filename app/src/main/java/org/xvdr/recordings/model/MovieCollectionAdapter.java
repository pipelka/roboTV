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
import org.xvdr.robotv.client.model.Movie;

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
            return lhs.getStartTime() > rhs.getStartTime() ? -1 : 1;
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

    private interface MovieIterator {
        boolean iterate(ArrayObjectAdapter adapter, Movie movie);
    }

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

            if(item.getRecordingId().equals(movie.getRecordingId())) {
                return item;
            }
        }

        return null;
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew) {
        return getCategory(category, addNew, mCardPresenter);
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

    private void add(Movie movie) {
        // add into "latest" category
        Movie item = movieExists(mLatest, movie);

        if(item != null) {
            item.setArtwork(movie);
            item.setEpisodeCount(1);
        }
        else {
            movie.setEpisodeCount(1);
            mLatest.add(movie);
        }

        if(movie.isTvShow()) {
            addEpisode(movie);
            return;
        }

        item = addMovie(movie);
        if(item != null) {
            item.setEpisodeCount(1);
        }
    }

    private Movie addMovie(Movie movie) {
        ArrayObjectAdapter row = getCategory(movie.getFolder(), true);

        if(row == null) {
            return null;
        }

        Movie item = movieExists(row, movie);

        if(item != null) {
            item.setArtwork(movie);
            return item;
        }

        row.add(movie);
        return movie;
    }

    private void addEpisode(Movie episode) {
        // check if series item already exists
        for(int i = 0; i < mTvShows.size(); i++) {
            Movie m = (Movie) mTvShows.get(i);
            if(m.getTitle().equals(episode.getTitle())) {
                m.setEpisodeCount(m.getEpisodeCount() + 1);
                return;
            }
        }

        // create a new item for this series
        Movie series = new Movie(0x15, episode.getTitle(), "", "", 0);
        series.setPosterUrl(episode.getPosterUrl());
        series.setBackgroundUrl(episode.getBackgroundUrl());
        series.setEpisodeCount(1);
        series.setSeriesHeader();

        mTvShows.add(series);
    }

    private void iterateAll(MovieIterator iterator) {
        // all rows
        for(int row = 0; row < size(); row++) {
            ListRow listrow = (ListRow)get(row);
            ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) listrow.getAdapter();

            // all items in the row
            for(int i = 0; i < rowAdapter.size();) {
                Object item = rowAdapter.get(i);

                if(item instanceof Movie) {
                    Movie m = (Movie) item;
                    if(iterator.iterate(rowAdapter, m)) {
                        i = 0;
                        continue;
                    }
                }

                i++;
            }
        }
    }

    public void remove(final Movie movie) {
        iterateAll(new MovieIterator() {
            @Override
            public boolean iterate(ArrayObjectAdapter adapter, Movie m) {
                String id = m.getRecordingId();
                if(!TextUtils.isEmpty(id) && id.equals(movie.getRecordingId())) {
                    adapter.remove(m);
                    return true;
                }

                return false;
            }
        });
    }

    synchronized public void load(Collection<Movie> movieCollection) {
        // reset count
        iterateAll(new MovieIterator() {
            @Override
            public boolean iterate(ArrayObjectAdapter adapter, Movie m) {
                m.setEpisodeCount(0);
                return false;
            }
        });

        // insert all movies
        if(movieCollection != null) {
            for(Movie movie : movieCollection) {
                add(movie);
            }
        }

        // check for removed entries
        iterateAll(new MovieIterator() {
            @Override
            public boolean iterate(ArrayObjectAdapter adapter, Movie m) {
                if(m.getEpisodeCount() == 0) {
                    adapter.remove(m);
                    return true;
                }

                return false;
            }
        });

        // update all rows
        for(int i = 0; i < size();) {
            ListRow listRow = (ListRow)get(i);
            ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) listRow.getAdapter();

            if(rowAdapter.size() == 0) {
                super.remove(listRow);
                i = 0;
                continue;
            }

            rowAdapter.notifyArrayItemRangeChanged(0, rowAdapter.size() - 1);
            i++;
        }
    }
}
