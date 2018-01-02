package org.robotv.recordings.model;

import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.text.TextUtils;

import org.robotv.recordings.presenter.IconActionPresenter;
import org.robotv.recordings.presenter.MoviePresenter;
import org.robotv.recordings.presenter.LatestCardPresenter;
import org.robotv.recordings.presenter.TimerPresenter;
import org.robotv.robotv.R;
import org.robotv.client.Connection;
import org.robotv.client.model.Event;
import org.robotv.client.model.Movie;
import org.robotv.client.model.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class MovieCollectionAdapter extends SortedArrayObjectAdapter {

    static public Comparator<Event> compareTimestamps = new Comparator<Event>() {
        @Override
        public int compare(Event lhs, Event rhs) {
            if(lhs.getStartTime() == rhs.getStartTime()) {
                return 0;
            }

            return lhs.getStartTime() > rhs.getStartTime() ? -1 : 1;
        }
    };

    private static Comparator<Event> compareTimestampsReverse = new Comparator<Event>() {
        @Override
        public int compare(Event lhs, Event rhs) {
            if(lhs.getStartTime() == rhs.getStartTime()) {
                return 0;
            }

            return lhs.getStartTime() < rhs.getStartTime() ? -1 : 1;
        }
    };

    private static Comparator<Row> compareCategories = new Comparator<Row>() {
        @Override
        public int compare(Row lhs, Row rhs) {
            HeaderItem lhsHeader = lhs.getHeaderItem();
            HeaderItem rhsHeader = rhs.getHeaderItem();
            int r;

            if(lhsHeader.getId() <= 9 && rhsHeader.getId() <= 9) {
                if(lhsHeader.getId() == rhsHeader.getId()) {
                    r = 0;
                }
                else {
                    r = (lhsHeader.getId() < rhsHeader.getId()) ? -1 : 1;
                }
            }
            else if(lhsHeader.getId() <= 9) {
                r = -1;
            }
            else if(rhsHeader.getId() <= 9) {
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
    final private IconActionPresenter iconActionPresenter;

    private ArrayObjectAdapter mLatest;
    private ArrayObjectAdapter mTvShows;
    private Context mContext;

    public MovieCollectionAdapter(Context context, Connection connection) {
        super(compareCategories, new ListRowPresenter());
        mContext = context;
        mCardPresenter = new MoviePresenter(connection);
        mLatestCardPresenter = new LatestCardPresenter(connection);
        timerPresenter = new TimerPresenter(connection);
        iconActionPresenter = new IconActionPresenter(250, 220);

        clear();
    }

    @Override
    public void clear() {
        super.clear();
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

        int id = (rowId == -1) ? size() + 10 : rowId;
        HeaderItem header = new HeaderItem(id, category);

        ArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(
                reverse ? compareTimestampsReverse : compareTimestamps,
                presenter);

        listrow = new ListRow(header, listRowAdapter);
        listrow.setId(id);

        add(listrow);

        return listRowAdapter;
    }

    public void add(Movie movie) {
        // sanity check
        if(TextUtils.isEmpty(movie.getTitle())) {
            return;
        }

        // add into "latest" category
        Movie item = movieExists(getLatestCategory(), movie);

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
        ArrayObjectAdapter shows = getTvShowCategory();

        // check if series item already exists
        for(int i = 0; i < shows.size(); i++) {
            Movie m = (Movie) shows.get(i);

            if(TextUtils.isEmpty(m.getTitle()) || TextUtils.isEmpty(episode.getTitle())) {
                continue;
            }

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

        shows.add(series);
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

        if(timerAdapter == null || timers == null) {
            return;
        }

        ArrayList<EpisodeTimer> episodeTimers = new ArrayList<>();
        ArrayList<Timer> newTimers = new ArrayList<>();

        // prepare timers (categorize)
        for(Timer timer : timers) {

            if(timer.isSearchTimer()) {
                boolean added = false;

                for(EpisodeTimer episodeTimer: episodeTimers) {
                    if(episodeTimer.add(timer)) {
                        added = true;
                        break;
                    }
                }

                if(!added) {
                    episodeTimers.add(new EpisodeTimer(timer));
                }
            }
            else {
                newTimers.add(timer);
            }
        }

        // add episode timers
        for(EpisodeTimer timer: episodeTimers) {
            newTimers.add(timer);
        }

        // remove items
        int diff = timerAdapter.size() - newTimers.size();
        if(diff > 0) {
            timerAdapter.removeItems(timerAdapter.size() - diff, diff);
        }

        Collections.sort(newTimers, compareTimestampsReverse);

        // add or replace timers
        int index = 0;
        for(Timer timer: newTimers) {
            if(timer == null) {
                continue;
            }

            if(index < timerAdapter.size()) {
                Timer t = (Timer) timerAdapter.get(index);
                if(t != null && !t.equals(timer)) {
                    timerAdapter.replace(index, timer);
                }
            }
            else {
                timerAdapter.add(timer);
            }
            index++;
        }

        timerAdapter.add(new IconAction(
                        100,
                        R.drawable.ic_add_circle_outline_white_48dp,
                        mContext.getString(R.string.schedule_recording)));

        updateRows();
    }

    private ArrayObjectAdapter getTvShowCategory() {
        if(mTvShows == null) {
            mTvShows = getCategory(mContext.getString(R.string.tv_shows), true, 1, mCardPresenter, false);
        }

        return mTvShows;
    }

    private ArrayObjectAdapter getLatestCategory() {
        if(mLatest == null) {
            mLatest = getCategory(mContext.getString(R.string.latest_movies), true, 0, mLatestCardPresenter, false);
        }

        return mLatest;
    }

    private ArrayObjectAdapter getTimerCategory() {
        int id = 900;
        String category = mContext.getString(R.string.schedule_timers);
        ArrayObjectAdapter adapter = getCategory(category, false);

        if(adapter != null) {
            return adapter;
        }

        ClassPresenterSelector selector = new ClassPresenterSelector();
        selector.addClassPresenter(Timer.class, timerPresenter);
        selector.addClassPresenter(IconAction.class, iconActionPresenter);

        HeaderItem header = new HeaderItem(id, category);
        adapter = new ArrayObjectAdapter(selector);

        ListRow row = new ListRow(header, adapter);
        row.setId(id);

        add(row);
        return adapter;
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
        updateRows();
    }

    private void updateRows() {
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
