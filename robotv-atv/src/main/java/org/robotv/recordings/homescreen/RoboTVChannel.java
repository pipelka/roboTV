package org.robotv.recordings.homescreen;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.robotv.client.Connection;
import org.robotv.client.MovieController;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.model.Event;
import org.robotv.client.model.Movie;
import org.robotv.player.Player;
import org.robotv.recordings.activity.PlayerActivity;
import org.robotv.recordings.activity.RecordingsActivity;
import org.robotv.recordings.fragment.VideoDetailsFragment;
import org.robotv.robotv.R;
import org.robotv.setup.SetupUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.TvContractCompat;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.WatchNextProgram;

public class RoboTVChannel {

    static private final ExecutorService threadPool = Executors.newCachedThreadPool();
    static private final String TAG = RoboTVChannel.class.getName();

    private final Context context;

    public RoboTVChannel(Context context) {
        this.context = context;
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

        // add recordings
        threadPool.execute(this::runMovieUpdate);
    }

    private void addEpgSearch(long channelId) {
        // add epg search programm
        Intent epgSearchIntent = new Intent(context, org.robotv.timers.activity.EpgSearchActivity.class);
        PreviewProgram.Builder epgSearchBuilder = new PreviewProgram.Builder();

        epgSearchBuilder.setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_EVENT)
                .setTitle(context.getString(R.string.schedule_recording))
                .setPosterArtUri(getUriToResource(context, R.drawable.ic_add_circle_outline_white_48dp))
                .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER)
                .setIntent(epgSearchIntent)
                .setSearchable(false);

        final ContentValues contentValues = epgSearchBuilder.build().toContentValues();
        context.getContentResolver().insert(TvContractCompat.PreviewPrograms.CONTENT_URI, contentValues);
    }

    static public void addWatchNext(Context context, Movie movie, long position, long duration) {
        removeWatchNext(context, movie);

        WatchNextProgram.Builder builder = new WatchNextProgram.Builder();
        builder.setType(TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
                .setId(movie.getRecordingId())
                .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                .setTitle(movie.getTitle())
                .setDescription(movie.getShortText())
                .setPosterArtUri(Uri.parse(movie.getBackgroundUrl()))
                .setIntent(getPlaybackIntent(context, movie))
                .setLastPlaybackPositionMillis((int) position)
                .setDurationMillis((int) duration)
                .setInternalProviderId(movie.getRecordingIdString());

        ContentValues contentValues = builder.build().toContentValues();
        context.getContentResolver()
                .insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, contentValues);
    }

    static public void removeWatchNext(Context context, Movie movie) {
        Uri uri = TvContractCompat.buildWatchNextProgramUri(movie.getRecordingId());
        context.getContentResolver().delete(uri, null, null);
    }

    static public Intent getPlaybackIntent(Context context, Movie movie) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
        intent.putExtra(VideoDetailsFragment.EXTRA_RECID, movie.getRecordingIdString());
        intent.putExtra(VideoDetailsFragment.EXTRA_SHOULD_AUTO_START, true);

        return intent;
    }

    synchronized public void updateFromCollection(ArrayList<Movie> list) {
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

        //createRecordings(list);
    }

    private void createRecordings(ArrayList<Movie> list) {

        context.getContentResolver().delete(TvContractCompat.RecordedPrograms.CONTENT_URI, null, null);
        String inputId = SetupUtils.getInputId(context);

        for(Movie movie: list) {
            ContentValues values = new ContentValues();

            values.put(TvContractCompat.RecordedPrograms.COLUMN_INPUT_ID, inputId);
            values.put(TvContractCompat.RecordedPrograms.COLUMN_SEARCHABLE, 1);
            values.put(TvContractCompat.RecordedPrograms.COLUMN_BROADCAST_GENRE, TvContractCompat.Programs.Genres.encode(movie.getFolder()));
            values.put(TvContractCompat.RecordedPrograms.COLUMN_CANONICAL_GENRE, TvContractCompat.Programs.Genres.encode(movie.getFolder()));

            if(movie.isTvShow()) {
                Event.SeasonEpisodeHolder holder = movie.getSeasionEpisode();
                values.put(TvContractCompat.RecordedPrograms.COLUMN_TITLE, movie.getTitle());
                values.put(TvContractCompat.RecordedPrograms.COLUMN_EPISODE_TITLE, movie.getShortText());
                values.put(TvContractCompat.RecordedPrograms.COLUMN_SEASON_DISPLAY_NUMBER, holder.season);
                values.put(TvContractCompat.RecordedPrograms.COLUMN_EPISODE_DISPLAY_NUMBER, holder.episode);
            }
            else {
                values.put(TvContractCompat.RecordedPrograms.COLUMN_TITLE, movie.getTitle());
                values.put(TvContractCompat.RecordedPrograms.COLUMN_SHORT_DESCRIPTION, movie.getShortText());
            }

            values.put(TvContractCompat.RecordedPrograms.COLUMN_LONG_DESCRIPTION, movie.getDescription());
            values.put(TvContractCompat.RecordedPrograms.COLUMN_POSTER_ART_URI, movie.getBackgroundUrl());
            values.put(TvContractCompat.RecordedPrograms.COLUMN_START_TIME_UTC_MILLIS, movie.getStartTime() * 1000);
            values.put(TvContractCompat.RecordedPrograms.COLUMN_END_TIME_UTC_MILLIS, (movie.getStartTime() * 1000) + movie.getDurationMs());
            values.put(TvContractCompat.RecordedPrograms.COLUMN_RECORDING_DURATION_MILLIS, movie.getDurationMs());
            values.put(TvContractCompat.RecordedPrograms.COLUMN_RECORDING_DATA_URI, Player.createRecordingUri(movie.getRecordingIdString(), 0).toString());
            values.put(TvContractCompat.RecordedPrograms.COLUMN_INTERNAL_PROVIDER_DATA, movie.getRecordingId());

            context.getContentResolver().insert(TvContractCompat.RecordedPrograms.CONTENT_URI, values);
        }
    }

    private void runMovieUpdate() {
        Connection connection = new Connection("roboTV:recommend", SetupUtils.getLanguage(context));

        if(!connection.open(SetupUtils.getServer(context))) {
            return;
        }

        MovieController controller = new MovieController(connection);

        long channelId = SetupUtils.getHomescreenChannelId(context);

        if(channelId == -1) {
            Log.d(TAG, "runMovieUpdate - channel not registered");
        }

        ArrayList<Movie> list = controller.load();

        if(list == null) {
            return;
        }

        ArtworkFetcher artworkFetcher = new ArtworkFetcher(connection, SetupUtils.getLanguage(context));

        for(Movie m : list) {
            try {
                artworkFetcher.fetchForEvent(m);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        connection.close();
        updateFromCollection(list);
    }

    /**
     * get uri to any resource type
     * @param context - context
     * @param resId - resource id
     * @throws Resources.NotFoundException if the given ID does not exist.
     * @return - Uri to resource by given id
     */
    public static Uri getUriToResource(Context context, int resId) throws Resources.NotFoundException {
        Resources res = context.getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + res.getResourcePackageName(resId)
                + '/' + res.getResourceTypeName(resId)
                + '/' + res.getResourceEntryName(resId));
    }
}
