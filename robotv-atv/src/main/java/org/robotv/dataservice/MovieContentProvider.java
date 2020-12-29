package org.robotv.dataservice;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;

import org.robotv.client.Connection;
import org.robotv.client.PacketAdapter;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.model.Movie;
import org.robotv.msgexchange.Packet;
import org.robotv.recordings.activity.DetailsActivity;
import org.robotv.recordings.fragment.VideoDetailsFragment;
import org.robotv.setup.SetupUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

public class MovieContentProvider extends ContentProvider {

    public static final String KEY_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_DESCRIPTION = SearchManager.SUGGEST_COLUMN_TEXT_2;
    public static final String KEY_ICON = SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE;
    public static final String KEY_DATA_TYPE = SearchManager.SUGGEST_COLUMN_CONTENT_TYPE;
    public static final String KEY_VIDEO_WIDTH = SearchManager.SUGGEST_COLUMN_VIDEO_WIDTH;
    public static final String KEY_VIDEO_HEIGHT = SearchManager.SUGGEST_COLUMN_VIDEO_HEIGHT;
    public static final String KEY_PRODUCTION_YEAR = SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR;
    public static final String KEY_COLUMN_DURATION = SearchManager.SUGGEST_COLUMN_DURATION;
    public static final String KEY_ACTION = SearchManager.SUGGEST_COLUMN_INTENT_ACTION;
    public static final String KEY_DATA = SearchManager.SUGGEST_COLUMN_INTENT_DATA;

    static final int SEARCH_SUGGEST = 1;
    static final int MOVIE_IMAGE = 2;

    static final String TAG = MovieContentProvider.class.getName();

    static final String[] columns = {
        KEY_NAME,
        KEY_DESCRIPTION,
        KEY_ICON,
        KEY_DATA_TYPE,
        KEY_VIDEO_WIDTH,
        KEY_VIDEO_HEIGHT,
        KEY_PRODUCTION_YEAR,
        KEY_COLUMN_DURATION,
        KEY_ACTION,
        KEY_DATA,
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
    };

    // Creates a UriMatcher object.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI("org.robotv.dataservice", "movieimage/*", MOVIE_IMAGE);
        sUriMatcher.addURI("org.robotv.dataservice", "search_suggest_query", SEARCH_SUGGEST);
    }

    Connection connection;

    @Override
    public boolean onCreate() {
        connection = new Connection("roboTV:contentprovider", SetupUtils.getLanguage(getContext()));
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Log.d(TAG, "query: " + uri.toString());

        // Use the UriMatcher to see what kind of query we have and format the db query accordingly
        if (sUriMatcher.match(uri) == SEARCH_SUGGEST) {
            if (selectionArgs == null) {
                throw new IllegalArgumentException(
                        "selectionArgs must be provided for the Uri: " + uri);
            }
            Log.d(TAG, "search suggest: " + selectionArgs[0] + " URI: " + uri);
            return getSuggestions(selectionArgs[0]);
        }
        throw new IllegalArgumentException("Unknown Uri: " + uri);
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if(sUriMatcher.match(uri) != MOVIE_IMAGE) {
            return null;
        }

        Log.d(TAG, "openFile: " + uri.toString());

        String id = uri.getLastPathSegment();
        Log.d(TAG, "hash: " + id);

        Context context = getContext();

        if(context == null) {
            return null;
        }

        String filename = context.getCacheDir().getAbsolutePath() + "/image_manager_disk_cache/" + id;

        return ParcelFileDescriptor.open(new File(filename), MODE_READ_ONLY);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return "vnd.android.cursor.dir/vnd.org.robotv.dataservice.movies";
            case MOVIE_IMAGE:
                return "image/jpeg";
        }

        return null;
    }

    @Override
    public String[] getStreamTypes(@NonNull Uri uri, @NonNull String mimeTypeFilter) {
        if (sUriMatcher.match(uri) == MOVIE_IMAGE) {
            return new String[]{"image/jpeg"};
        }

        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    private Cursor getSuggestions(String query) {
        MatrixCursor cursor = new MatrixCursor(columns);
        Context context = getContext();

        if(!connection.open(SetupUtils.getServer(context))) {
            return null;
        }

        ArtworkFetcher artworkFetcher = new ArtworkFetcher(connection, SetupUtils.getLanguage(context));

        Packet req = connection.CreatePacket(Connection.RECORDINGS_SEARCH, Connection.CHANNEL_REQUEST_RESPONSE);
        req.putString(query);

        Packet resp = connection.transmitMessage(req);

        connection.close();

        // no results
        if(resp == null || resp.eop()) {
            return cursor;
        }

        // results
        while(!resp.eop()) {
            Movie movie = PacketAdapter.toMovie(resp);
            Log.d(TAG, movie.getTitle());

            try {
                artworkFetcher.fetchForEvent(movie);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            cursor.addRow(mapMovieToRow(context, movie));
        }
        return cursor;
    }

    static Object[] mapMovieToRow(Context context, Movie movie) {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);

        String url = TextUtils.isEmpty(movie.getPosterUrl()) ? "" : movie.getPosterUrl();

        File file = null;

        try {
            file = Glide
                .with(context)
                .downloadOnly()
                .load(url)
                .submit(270, 400)
                .get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        String fileUrl = "";

        if(file != null) {
            Uri uri = Uri.fromFile(file);
            fileUrl = "content://org.robotv.dataservice/movieimage/" + uri.getLastPathSegment();
        }

        return new Object[]{
                movie.getTitle(),
                movie.getDescription(),
                fileUrl,
                "video/avc",
                1920,
                1080,
                0,
                movie.getDuration() * 1000L,
                "android.intent.action.VIEW",
                movie.getRecordingIdString(),
                movie.getRecordingIdString(),
                movie.getRecordingIdString()
        };
    }
}
