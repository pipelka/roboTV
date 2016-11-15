package org.xvdr.recordings.model;

import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.text.TextUtils;

import org.xvdr.recordings.presenter.MoviePresenter;
import org.xvdr.recordings.presenter.LatestCardPresenter;
import org.xvdr.recordings.presenter.TimerPresenter;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.model.Event;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.robotv.client.model.Timer;

import java.util.Collection;
import java.util.Comparator;

public class MovieCollectionAdapter extends SortedArrayObjectAdapter {

    static public Comparator<Event> compareTimestamps = new Comparator<Event>() {
        @Override
        public int compare(Event lhs, Event rhs) {
            return lhs.getStartTime() > rhs.getStartTime() ? -1 : 1;
        }
    };

    private static Comparator<Event> compareTimestampsReverse = new Comparator<Event>() {
        @Override
        public int compare(Event lhs, Event rhs) {
            return lhs.getStartTime() < rhs.getStartTime() ? -1 : 1;
        }
    };

    private static Comparator<Row> compareCategories = new Comparator<Row>() {
        @Override
        public int compare(Row lhs, Row rhs) {
            HeaderItem lhsHeader = lhs.getHeaderItem();
            HeaderItem rhsHeader = rhs.getHeaderItem();
            int r;

            if(lhsHeader.getId() == 0) {
                r = -1;
            }
            else if(rhsHeader.getId() == 0) {
                r = 1;
            }
            else if(lhsHeader.getId() == 1) {
                r = -1;
            }
            else if(rhsHeader.getId() == 1) {
                r = 1;
            }
            else if(lhsHeader.getId() == 2) {
                r = -1;
            }
            else if(rhsHeader.getId() == 2) {
                r = 1;
            }
            else if(lhsHeader.getId() >= 900 && rhsHeader.getId() >= 900) {
                if(lhsHeader.getId() == rhsHeader.getId()) {
                    r = 0;
                }
                else {
                    r = (lhsHeader.getId() < rhsHeader.getId()) ? -1 : 1;
                }
            }
            else if(lhsHeader.getId() >= 900) {
                r = 1;
            }
            else if(rhsHeader.getId() >= 900) {
                r = -1;
            }
            else {
                r = lhsHeader.getName().compareTo(rhsHeader.getName());
            }

            return r;
        }
    };

    private interface MovieIterator {
        boolean iterate(ArrayObjectAdapter adapter, Movie movie);
    }

    final private MoviePresenter mCardPresenter;
    final private LatestCardPresenter mLatestCardPresenter;
    final private TimerPresenter timerPresenter;

    private ArrayObjectAdapter mLatest;
    private ArrayObjectAdapter mTvShows;
    private Context mContext;

    public MovieCollectionAdapter(Context context) {
        super(compareCategories, new ListRowPresenter());
        mContext = context;
        mCardPresenter = new MoviePresenter();
        mLatestCardPresenter = new LatestCardPresenter();
        timerPresenter = new TimerPresenter();

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

            if(item.getRecordingId() == movie.getRecordingId()) {
                return item;
            }
        }

        return null;
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew) {
        return getCategory(category, addNew, mCardPresenter);
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew, Presenter presenter) {
        return getCategory(category, addNew, -1, presenter, false);
    }

    private ArrayObjectAdapter getCategory(String category, boolean addNew, int rowId, Presenter presenter, boolean reverse) {
        if(TextUtils.isEmpty(category)) {
            return null;
        }

        Row listrow;

        for(int i = 0; i < size(); i++) {
            listrow = (Row)get(i);

            if(listrow.getHeaderItem().getName().equalsIgnoreCase(category)) {
                if(listrow instanceof ListRow) {
                    return (ArrayObjectAdapter) ((ListRow)listrow).getAdapter();
                }
                return null;
            }
        }

        if(!addNew) {
            return null;
        }

        int id = (rowId == -1) ? size() : rowId;
        HeaderItem header = new HeaderItem(id, category);

        ArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(
                reverse ? compareTimestampsReverse : compareTimestamps,
                presenter);

        listrow = new ListRow(header, listRowAdapter);
        listrow.setId(id);

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
            Row listrow = (Row)get(row);

            if(!(listrow instanceof ListRow)) {
                continue;
            }

            ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) ((ListRow)listrow).getAdapter();

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
                int id = m.getRecordingId();
                if(id == movie.getRecordingId()) {
                    adapter.remove(m);
                    return true;
                }

                return false;
            }
        });
    }

    public void loadTimers(Collection<Timer> timers) {
        ArrayObjectAdapter timerAdapter = getTimerCategory();

        if(timerAdapter == null) {
            return;
        }

        timerAdapter.clear();

        if(timers == null) {
            return;
        }

        // insert all timers
        for(Timer timer : timers) {
            timerAdapter.add(timer);
        }
    }

    public boolean hasTimerCategory() {
        return
            getCategory(mContext.getString(R.string.schedule_timers), false) != null ||
            getCategory(mContext.getString(R.string.search_timers), false) != null;
    }

    private ArrayObjectAdapter getTimerCategory() {
        return getCategory(mContext.getString(R.string.schedule_timers), true, 901, timerPresenter, true);
    }

    private ArrayObjectAdapter getSearchTimerCategory() {
        return getCategory(mContext.getString(R.string.search_timers), true, 901, timerPresenter, true);
    }

    public void loadMovies(Collection<Movie> movieCollection) {
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
            Row listRow = (Row)get(i);

            if(!(listRow instanceof ListRow)) {
                i++;
                continue;
            }

            ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) ((ListRow)listRow).getAdapter();

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
