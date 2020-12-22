package org.robotv.recordings.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.SearchOrbView;

import org.robotv.dataservice.DataService;
import org.robotv.dataservice.DataServiceClient;
import org.robotv.msgexchange.Packet;
import org.robotv.recordings.activity.DetailsActivity;
import org.robotv.client.model.Movie;
import org.robotv.recordings.model.MovieCollectionAdapter;
import org.robotv.client.PacketAdapter;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.Connection;
import org.robotv.setup.SetupUtils;
import org.robotv.ui.SearchProgressFragment;

import java.util.ArrayList;

public class MovieSearchFragment extends SearchProgressFragment {

    private static final String TAG = MovieSearchFragment.class.getName();

    private class SearchRunnable implements Runnable {
        private String query;

        @Override
        public void run() {
            Packet req = connection.CreatePacket(Connection.RECORDINGS_SEARCH, Connection.CHANNEL_REQUEST_RESPONSE);
            req.putString(query);

            final Packet resp = connection.transmitMessage(req);

            ArrayList<Movie> list = new ArrayList<>();

            if(resp != null && !resp.eop()) {
                while (!resp.eop()) {
                    Movie movie = PacketAdapter.toMovie(resp);
                    list.add(movie);
                }
            }

            getActivity().runOnUiThread(() -> {
                // no results
                if(list.size() == 0) {
                    HeaderItem header = new HeaderItem(getString(R.string.no_search_results, query));
                    ListRow listRow = new ListRow(header, new ArrayObjectAdapter());
                    rowsAdapter.clear();
                    rowsAdapter.add(listRow);
                } else {
                    rowsAdapter.loadMovies(list);
                }

                showProgress(false);
            });
        }

        public void setSearchQuery(String query) {
            this.query = query;
        }
    }

    private MovieCollectionAdapter rowsAdapter;
    private final SearchRunnable searchRunnable = new SearchRunnable();
    private Connection connection;
    private DataServiceClient client;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setSearchAffordanceColorsInListening(new SearchOrbView.Colors(
                Utils.getColor(getContext(), R.color.recordings_search_button_color),
                Utils.getColor(getContext(), R.color.primary_color_light),
                Utils.getColor(getContext(), R.color.primary_color)
        ));

        setSearchResultProvider(this);

        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            Movie movie = (Movie) item;

            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
            startActivity(intent);
        });

        connection = new Connection("roboTV search movies");
        rowsAdapter = new MovieCollectionAdapter(getActivity(), connection);

        connection.open(SetupUtils.getServer(getActivity()));

        client = new DataServiceClient(getContext(), new DataService.Listener() {
            @Override
            public void onConnected(DataService service) {
                rowsAdapter.setOnLongClickListener(movie -> RecordingsFragment.openDetailsMenu(MovieSearchFragment.this.getActivity(), service, movie, R.id.container));
            }

            @Override
            public void onConnectionError(DataService service) {
            }

            @Override
            public void onMovieUpdate(DataService service) {
                search();
            }

            @Override
            public void onTimersUpdated(DataService service) {
            }
        });

        client.bind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(client != null) {
            client.unbind();
        }

        connection.close();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return rowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(!super.onQueryTextSubmit(query)){
            return false;
        }

        searchRunnable.setSearchQuery(query);
        search();

        return true;
    }

    private void search() {
        new Thread(searchRunnable).start();
    }
}
