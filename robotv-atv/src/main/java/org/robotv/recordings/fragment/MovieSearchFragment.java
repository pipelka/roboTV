package org.robotv.recordings.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.SearchOrbView;

import android.text.TextUtils;

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

public class MovieSearchFragment extends SearchSupportFragment implements SearchSupportFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 300;

    private DataServiceClient client;

    private class SearchRunnable implements Runnable {

        private String query;

        @Override
        public void run() {
            Packet req = mConnection.CreatePacket(Connection.RECORDINGS_SEARCH, Connection.CHANNEL_REQUEST_RESPONSE);
            req.putString(query);

            final Packet resp = mConnection.transmitMessage(req);

            getActivity().runOnUiThread(() -> {
                mRowsAdapter.clear();

                // no results
                if (resp == null || resp.eop()) {
                    HeaderItem header = new HeaderItem(getString(R.string.no_search_results, query));
                    ListRow listRow = new ListRow(header, new ArrayObjectAdapter());
                    mRowsAdapter.add(listRow);
                } else {
                    // results
                    while (!resp.eop()) {
                        Movie movie = PacketAdapter.toMovie(resp);
                        mRowsAdapter.add(movie);
                    }
                }
            });
        }

        void setSearchQuery(String newQuery) {
            query = newQuery;
        }
    }

    private MovieCollectionAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private SearchRunnable mDelayedLoad;
    private Connection mConnection;

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

        mDelayedLoad = new SearchRunnable();
        mConnection = new Connection("roboTV search movies");

        mConnection.open(SetupUtils.getServer(getActivity()));
        mRowsAdapter = new MovieCollectionAdapter(getActivity(), mConnection);

        client = new DataServiceClient(getContext(), new DataService.Listener() {
            @Override
            public void onConnected(DataService service) {
                mRowsAdapter.setOnLongClickListener(movie -> RecordingsFragment.openDetailsMenu(MovieSearchFragment.this.getActivity(), service, movie, R.id.search));
            }

            @Override
            public void onConnectionError(DataService service) {
            }

            @Override
            public void onMovieUpdate(DataService service) {
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

        mConnection.close();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        /*mRowsAdapter.clear();

        if(!TextUtils.isEmpty(newQuery)) {
            mDelayedLoad.setSearchQuery(newQuery);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }*/

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mRowsAdapter.clear();

        if(!TextUtils.isEmpty(query)) {
            mDelayedLoad.setSearchQuery(query);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }

        return true;
    }
}
