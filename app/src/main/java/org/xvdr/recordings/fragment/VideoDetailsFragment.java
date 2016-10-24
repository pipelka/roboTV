package org.xvdr.recordings.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.BaseOnItemViewClickedListener;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import org.xvdr.recordings.activity.CoverSearchActivity;
import org.xvdr.recordings.activity.PlayerActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.model.MovieCollectionAdapter;
import org.xvdr.recordings.model.SortedArrayObjectAdapter;
import org.xvdr.recordings.presenter.ActionPresenterSelector;
import org.xvdr.recordings.presenter.ColorAction;
import org.xvdr.recordings.presenter.DetailsDescriptionPresenter;
import org.xvdr.recordings.presenter.LatestCardPresenter;
import org.xvdr.recordings.presenter.MoviePresenter;
import org.xvdr.recordings.util.PicassoBackgroundManagerTarget;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Collection;
import java.util.Comparator;

public class VideoDetailsFragment extends DetailsFragment {

    public static final String TAG = "VideoDetailsFragment";
    public static final String EXTRA_MOVIE = "extra_movie";
    public static final String EXTRA_SHOULD_AUTO_START = "extra_should_auto_start";

    private static final int ACTION_WATCH = 1;
    private static final int ACTION_EDIT = 2;
    private static final int ACTION_MOVE = 3;

    private Movie mSelectedMovie;

    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private DataServiceClient dataClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataClient = new DataServiceClient(getActivity());
        dataClient.bind();

        mSelectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(EXTRA_MOVIE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dataClient.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();

        initBackground();
        new DetailRowBuilderTask().execute(mSelectedMovie);
    }

    private void initBackground() {
        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        if(mSelectedMovie != null && !TextUtils.isEmpty(mSelectedMovie.getBackgroundImageUrl())) {
            updateBackground(mSelectedMovie.getBackgroundImageUrl());
        }
    }

    protected void updateBackground(String url) {
        if(url == null || url.isEmpty()) {
            return;
        }

        Picasso.with(getActivity())
        .load(url)
        .error(new ColorDrawable(Utils.getColor(getActivity(), R.color.recordings_background)))
        .resize(mMetrics.widthPixels, mMetrics.heightPixels)
        .into(mBackgroundTarget);
    }

    private class DetailRowBuilderTask extends AsyncTask<Movie, Integer, DetailsOverviewRow> {
        @Override
        protected DetailsOverviewRow doInBackground(Movie... movies) {
            mSelectedMovie = movies[0];
            String url = mSelectedMovie.getCardImageUrl();

            DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);

            try {
                if(!(url == null || url.isEmpty())) {
                    Bitmap poster = Picasso.with(getActivity())
                                    .load(url)
                                    .resize(Utils.dpToPx(R.integer.artwork_poster_width, getActivity()),
                                            Utils.dpToPx(R.integer.artwork_poster_height, getActivity()))
                                    .error(getResources().getDrawable(R.drawable.recording_unkown, null))
                                    .placeholder(getResources().getDrawable(R.drawable.recording_unkown, null))
                                    .centerCrop()
                                    .get();
                    row.setImageBitmap(getActivity(), poster);
                }
                else {
                    row.setImageDrawable(getResources().getDrawable(R.drawable.recording_unkown, null));
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }

            return row;
        }

        @Override
        protected void onPostExecute(DetailsOverviewRow detailRow) {
            if(detailRow == null) {
                return;
            }

            SparseArrayObjectAdapter actions = new SparseArrayObjectAdapter();

            actions.set(0,
                    new Action(
                            ACTION_WATCH,
                            null,
                            getResources().getString(R.string.watch),
                            getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp, null)
                    )
            );

            detailRow.setActionsAdapter(actions);

            ClassPresenterSelector ps = new ClassPresenterSelector();
            DetailsOverviewRowPresenter dorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
            // set detail background and style
            dorPresenter.setBackgroundColor(Utils.getColor(getActivity(), R.color.primary_color));
            dorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
                @Override
                public void onActionClicked(Action action) {
                    if(action.getId() == ACTION_WATCH) {
                        playbackMovie(mSelectedMovie);
                    }
                }
            });

            ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
            ps.addClassPresenter(ListRow.class,
                                 new ListRowPresenter());


            ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);
            adapter.add(detailRow);
            setExtraActions(adapter);
            loadRelatedContent(adapter);
            setAdapter(adapter);

            setOnItemViewClickedListener(new BaseOnItemViewClickedListener() {
                @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
                    if(item instanceof ColorAction) {
                        ColorAction action = (ColorAction) item;
                        if(action.getId() == ACTION_EDIT) {
                            Intent intent = new Intent(getActivity(), CoverSearchActivity.class);
                            intent.putExtra(EXTRA_MOVIE, mSelectedMovie);
                            startActivity(intent);
                            getActivity().finishAndRemoveTask();
                        }
                        else if(action.getId() == ACTION_MOVE) {
                        }
                    }
                    else if(item instanceof Movie) {
                        playbackMovie((Movie) item);
                    }
                }
            });
        }

        private void playbackMovie(Movie movie) {
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra(EXTRA_MOVIE, movie);
            intent.putExtra(EXTRA_SHOULD_AUTO_START, true);
            startActivity(intent);
            getActivity().finishAndRemoveTask();
        }

        private void loadRelatedContent(ArrayObjectAdapter adapter) {
            DataService service = dataClient.getService();

            if(service == null) {
                Log.d(TAG, "service is null");
                return;
            }

            Collection<Movie> collection = service.getRelatedContent(mSelectedMovie);

            if(collection == null) {
                return;
            }

            SortedArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(
                    MovieCollectionAdapter.compareTimestamps,
                    new LatestCardPresenter());

            listRowAdapter.addAll(collection);

            HeaderItem header = new HeaderItem(0, getString(R.string.related_movies));
            adapter.add(new ListRow(header, listRowAdapter));
        }

    }

    private void setExtraActions(ArrayObjectAdapter adapter) {
        ActionPresenterSelector presenterSelector = new ActionPresenterSelector();
        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter(presenterSelector);
        actionAdapter.add(
                new ColorAction(
                        ACTION_EDIT,
                        getResources().getString(R.string.change_cover),
                        "",
                        getResources().getDrawable(R.drawable.ic_style, null)
                ).setColor(Utils.getColor(getActivity(), R.color.default_background))
        );
        actionAdapter.add(
                new ColorAction(
                        ACTION_MOVE,
                        getResources().getString(R.string.move_folder),
                        "",
                        getResources().getDrawable(R.drawable.ic_folder, null)
                ).setColor(Utils.getColor(getActivity(), R.color.default_background))
        );

        ListRow listRow = new ListRow(new HeaderItem(getString(R.string.movie_actions)), actionAdapter);

        adapter.add(listRow);
    }

}
