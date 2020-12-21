package org.robotv.recordings.homescreen;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.robotv.client.Connection;
import org.robotv.client.MovieController;
import org.robotv.client.model.Movie;
import org.robotv.recordings.activity.PlayerActivity;
import org.robotv.recordings.activity.RecordingsActivity;
import org.robotv.recordings.fragment.VideoDetailsFragment;
import org.robotv.robotv.R;
import org.robotv.setup.SetupUtils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.TvContractCompat;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.PreviewProgram;

public class RoboTVChannel {

    static private final ExecutorService threadPool = Executors.newCachedThreadPool();
    static private final String TAG = RoboTVChannel.class.getName();

    private final Context context;
    private final Connection connection;

    public RoboTVChannel(Context context, Connection connection) {
        this.context = context;
        this.connection = connection;
    }

    public void create() {
        if(SetupUtils.getHomescreenChannelId(context) != -1) {
            return;
        }

        Intent intent = new Intent(context, RecordingsActivity.class);

        Channel.Builder builder = new Channel.Builder();

        builder
            .setType(TvContractCompat.Channels.TYPE_PREVIEW)
            .setDisplayName(context.getString(R.string.recordings))
            .setSearchable(true)
            .setAppLinkIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        Uri channelUri = context.getContentResolver().insert(
            TvContractCompat.Channels.CONTENT_URI, builder.build().toContentValues()
        );

        long channelId = ContentUris.parseId(channelUri);
        SetupUtils.setHomescreenChannelId(context, channelId);

        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_robotv_channel_logo_light);

        ChannelLogoUtils.storeChannelLogo(context, channelId, icon);
        TvContractCompat.requestChannelBrowsable(context, channelId);
    }

    public void update() {
        Log.d(TAG, "update");

        long channelId = SetupUtils.getHomescreenChannelId(context);

        Intent intent = new Intent(context, RecordingsActivity.class);

        Channel.Builder builder = new Channel.Builder();

        builder
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(context.getString(R.string.recordings))
                .setSearchable(true)
                .setAppLinkIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        context.getContentResolver().update(
                TvContractCompat.buildChannelUri(channelId),
                builder.build().toContentValues(),
                null,
                null
        );

        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_robotv_channel_logo_light);

        ChannelLogoUtils.storeChannelLogo(context, channelId, icon);

        if(channelId == -1) {
            return;
        }

        MovieController controller = new MovieController(connection);
        threadPool.execute(() -> runMovieUpdate(controller));
    }

    public void updateFromCollection(ArrayList<Movie> list) {
        if(list == null) {
            return;
        }

        long channelId = SetupUtils.getHomescreenChannelId(context);

        if(channelId == -1) {
            return;
        }

        context.getContentResolver().delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null);

        list.sort(MovieController.compareTimestamps);

        int count = Math.min(list.size(), 50);

        for(int i = 0; i < count; i++) {
            Movie movie = list.get(i);

            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
            intent.putExtra(VideoDetailsFragment.EXTRA_RECID, movie.getRecordingIdString());
            intent.putExtra(VideoDetailsFragment.EXTRA_SHOULD_AUTO_START, true);

            PreviewProgram.Builder builder = new PreviewProgram.Builder();

            if(movie.isTvShow()) {
                builder.setChannelId(channelId)
                        /*.setId(movie.getRecordingId())*/
                        .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                        .setTitle(movie.getTitle())
                        .setEpisodeTitle(movie.getShortText())
                        .setSeasonNumber(movie.getSeasionEpisode().season)
                        .setEpisodeNumber(movie.getSeasionEpisode().episode)
                        .setLongDescription(movie.getDescription())
                        .setPosterArtUri(Uri.parse(movie.getPosterUrl()))
                        .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER)
                        .setDurationMillis(movie.getDuration() * 1000)
                        .setIntent(intent)
                        .setSearchable(true)
                        .setInternalProviderId(movie.getRecordingIdString());

            }
            else {
                builder.setChannelId(channelId)
                        .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                        .setTitle(movie.getTitle())
                        .setDescription(movie.getDescription())
                        .setPosterArtUri(Uri.parse(movie.getPosterUrl()))
                        .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER)
                        .setDurationMillis(movie.getDuration() * 1000)
                        .setIntent(intent)
                        .setSearchable(true)
                        .setInternalProviderId(movie.getRecordingIdString());
            }

            final ContentValues contentValues = builder.build().toContentValues();
            context.getContentResolver().insert(TvContractCompat.PreviewPrograms.CONTENT_URI, contentValues);
        }

        Log.d(TAG, "added " + count + " preview items");
    }

    private void runMovieUpdate(MovieController controller) {
        long channelId = SetupUtils.getHomescreenChannelId(context);

        if(channelId == -1) {
            Log.d(TAG, "runMovieUpdate - channel not registered");
        }

        ArrayList<Movie> list = controller.load();
        updateFromCollection(list);
    }
}
