package org.xvdr.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import org.xvdr.jniwrap.Packet;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.Event;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.client.Channels;
import org.xvdr.robotv.client.Connection;
import org.xvdr.timers.activity.TimerActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
    DVB Content Genres

    MovieDrama                          0x10
      Detective/Thriller                0x11
      Adventure/Western/War             0x12
      SF/Fantasy/Horror                 0x13
      Comedy                            0x14
      Soap/Melodrama/Folk               0x15
      Romance                           0x16
      Religious/Historical              0x17
      Adult Movie/Drama                 0x18

    NewsCurrentAffairs                  0x20
      News/Weather Report               0x21
      News Magazine                     0x22
      Documentary                       0x23
      Discussion/Interview              0x24

    Show                                0x30
      Game Show/Quiz/Contest            0x31
      Variety Show                      0x32
      Talk Show                         0x33

    Sports                              0x40
      Special Event                     0x41
      Sport Magazine                    0x42
      Football/Soccer                   0x43
      Tennis/Squash                     0x44
      Team Sports                       0x45
      Athletics                         0x46
      Motor Sport                       0x47
      Water Sport                       0x48
      Winter Sports                     0x49
      Equestrian                        0x4A
      Martial Sports                    0x4B

    ChildrenYouth                       0x50
      Pre-school                        0x51
      Entertainment for 6 to 14         0x52
      Entertainment for 10 to 16        0x53
      Informational/Educational/School  0x54
      Cartoons/Puppets                  0x55

    MusicBalletDance                    0x60
    ArtsCulture                         0x70
      Performing Arts                   0x71
      Fine Arts                         0x72
      Religion                          0x73
      Popular Culture/Traditional Arts  0x74
      Literature                        0x75
      Film/Cinema                       0x76
      Experimental Film/Video           0x77
      Broadcasting/Press                0x78
      New Media                         0x79
      Arts/Culture Magazine             0x7A
      Fashion                           0x7B

    SocialPoliticalEconomics            0x80
      Magazine/Report/Documentary       0x81
      Economics/Social Advisory         0x82
      Remarkable People                 0x83

    EducationalScience                  0x90
      Nature/Animals/Environment        0x91
      Technology/Natural Sciences       0x92
      Medicine/Physiology/Psychology    0x93
      Foreign Countries/Expeditions     0x94
      Social/Spiritual Sciences         0x95
      Further Education                 0x96
      Languages                         0x97

    LeisureHobbies                      0xA0
      Tourism/Travel                    0xA1
      Handicraft                        0xA2
      Motoring                          0xA3
      Fitness & Health                  0xA4
      Cooking                           0xA5
      Advertisement/Shopping            0xA6
      Gardening                         0xA7

    Special                             0xB0
    gUserDefined                        0xF0
*/
public class ChannelSyncAdapter {

    public interface ProgressCallback {

        void onProgress(int done, int total);

        void onDone();
    }

    private static final String TAG = "ChannelSyncAdapter";

    private Context mContext;
    private Connection mConnection;
    private ArtworkFetcher mArtwork;
    private String mInputId;

    final private Packet mRequest;
    final private Packet mResponse;

    private static final SparseArray<String> mCanonicalGenre = new SparseArray<String>() {
        {
            append(0x10, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
            append(0x11, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
            append(0x12, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
            append(0x13, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
            append(0x14, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.COMEDY));
            append(0x15, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ENTERTAINMENT));
            append(0x16, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
            append(0x17, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.DRAMA));
            append(0x20, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.NEWS));
            append(0x21, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.NEWS));
            append(0x22, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.NEWS));
            append(0x23, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.TECH_SCIENCE));
            append(0x30, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ENTERTAINMENT));
            append(0x31, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ENTERTAINMENT));
            append(0x32, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ENTERTAINMENT));
            append(0x33, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ENTERTAINMENT));
            append(0x40, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x41, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x42, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x43, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x44, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x45, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x46, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x47, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x48, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x49, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x4A, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x4B, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
            append(0x50, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.FAMILY_KIDS));
            append(0x51, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.EDUCATION));
            append(0x52, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.FAMILY_KIDS));
            append(0x53, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.FAMILY_KIDS));
            append(0x54, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.EDUCATION));
            append(0x53, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.FAMILY_KIDS));
            append(0x60, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MUSIC));
            append(0x61, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MUSIC));
            append(0x62, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MUSIC));
            append(0x63, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MUSIC));
            append(0x64, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MUSIC));
            append(0x70, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
            append(0x71, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
            append(0x72, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
            append(0x74, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
            append(0x75, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
            append(0x76, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
            append(0x77, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
            append(0x78, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.NEWS));
            append(0x79, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.NEWS));
            append(0x7A, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
            append(0x81, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.TECH_SCIENCE));
            append(0x90, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.TECH_SCIENCE));
            //append(0x91, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ANIMAL_WILDLIFE));
            append(0x92, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.TECH_SCIENCE));
            append(0x93, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.TECH_SCIENCE));
            append(0x94, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.TRAVEL));
            append(0xA0, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.LIFE_STYLE));
            append(0xA1, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.TRAVEL));
            append(0xA2, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
            append(0xA3, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.LIFE_STYLE));
            append(0xA4, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.LIFE_STYLE));
            append(0xA5, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.LIFE_STYLE));
            append(0xA6, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SHOPPING));
            append(0xA7, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.LIFE_STYLE));
        }
    };

    private ProgressCallback mProgressCallback = null;
    private SyncChannelIconsTask mChannelIconsTask = null;

    public ChannelSyncAdapter(Context context, String inputId, Connection connection) {
        mContext = context;
        mConnection = connection;
        mInputId = inputId;

        mArtwork = new ArtworkFetcher(mConnection, SetupUtils.getLanguage(context));
        mRequest = mConnection.CreatePacket(Connection.XVDR_EPG_GETFORCHANNEL);
        mResponse = new Packet();
    }

    public void setProgressCallback(ProgressCallback callback) {
        mProgressCallback = callback;
    }

    public void syncChannels(boolean removeExisting) {
        final SparseArray<Long> existingChannels = new SparseArray<>();
        final ContentResolver resolver = mContext.getContentResolver();

        Log.i(TAG, "syncing channel list ...");

        // remove existing channels
        if(removeExisting)  {
            Uri uri = TvContract.buildChannelsUriForInput(mInputId);
            resolver.delete(uri, null, null);
        }

        // fetch existing channel list

        getExistingChannels(resolver, mInputId, existingChannels);

        // update or insert channels

        Channels list = new Channels();
        String language = SetupUtils.getLanguageISO3(mContext);

        list.load(mConnection, language);

        int i = 0;

        for(Channels.Entry entry : list) {

            // skip obsolete channels
            if(entry.name.endsWith("OBSOLETE")) {
                continue;
            }

            // epg search intent

            Intent intent = new Intent(mContext, TimerActivity.class);
            intent.putExtra("uid", entry.uid);
            intent.putExtra("name", entry.name);

            String link = "intent:" + intent.toUri(0);

            Uri channelUri;
            Long channelId = existingChannels.get(entry.uid);

            // channel entry
            ContentValues values = new ContentValues();
            values.put(TvContract.Channels.COLUMN_INPUT_ID, mInputId);

            values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, Integer.toString(entry.number));
            values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, entry.name);
            values.put(TvContract.Channels.COLUMN_SERVICE_ID, 0);
            values.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, 0);
            values.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, entry.uid);
            values.put(TvContract.Channels.COLUMN_SERVICE_TYPE, entry.radio ? TvContract.Channels.SERVICE_TYPE_AUDIO : TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO);
            values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_DVB_S2);
            values.put(TvContract.Channels.COLUMN_SEARCHABLE, 1);
            values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, Integer.toString(entry.uid));

            // channel link needs Android M
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                values.put(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI, getUriToResource(mContext, R.drawable.banner_timers).toString());
                values.put(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI, link);
                values.put(TvContract.Channels.COLUMN_APP_LINK_TEXT, mContext.getString(R.string.timer_title));
                values.put(TvContract.Channels.COLUMN_APP_LINK_COLOR, Utils.getColor(mContext, R.color.primary_color));
                values.put(TvContract.Channels.COLUMN_APP_LINK_ICON_URI, "");
            }

            // insert new channel
            if(channelId == null) {
                resolver.insert(TvContract.Channels.CONTENT_URI, values);
            }
            // update existing channel
            else {
                channelUri = TvContract.buildChannelUri(channelId);

                if(channelUri != null) {
                    resolver.update(channelUri, values, null, null);
                    existingChannels.remove(entry.uid);
                }
            }

            if(mProgressCallback != null) {
                mProgressCallback.onProgress(++i, list.size());
            }

        }

        // remove orphaned channels

        int size = existingChannels.size();

        for(i = 0; i < size; ++i) {
            Long channelId = existingChannels.valueAt(i);

            if(channelId == null) {
                continue;
            }

            Uri uri = TvContract.buildChannelUri(channelId);
            resolver.delete(uri, null, null);
        }

        if(mProgressCallback != null) {
            mProgressCallback.onDone();
        }

        Log.i(TAG, "synced channels");
    }

    public void syncChannelIcons() {
        // task already running
        if(mChannelIconsTask != null) {
            return;
        }

        Log.i(TAG, "syncing of channel icons started.");

        mChannelIconsTask = new SyncChannelIconsTask(mConnection, mContext, mInputId) {
            @Override
            protected Void doInBackground(Void... params) {
                return super.doInBackground(params);
            }

            @Override
            protected void onPostExecute(Void result) {
                mChannelIconsTask = null;
                Log.i(TAG, "finished syncing channel icons.");
            }

            @Override
            protected void onCancelled(Void result) {
                mChannelIconsTask = null;
                Log.i(TAG, "syncing of channel icons cancelled.");
            }
        };

        mChannelIconsTask.execute(null, null, null);
    }

    public void syncEPG() {
        SparseArray<Long> existingChannels = new SparseArray<>();
        getExistingChannels(mContext.getContentResolver(), mInputId, existingChannels);

        Log.i(TAG, "syncing epg ...");

        // fetch epg entries for each channel
        int size = existingChannels.size();

        ContentResolver resolver = mContext.getContentResolver();
        List<ContentValues> programs = new ArrayList<>();

        for(int i = 0; i < size; ++i) {

            programs.clear();
            fetchEPGForChannel(resolver, existingChannels.keyAt(i), existingChannels.valueAt(i), programs);

            if(programs.isEmpty()) {
                continue;
            }

            // populate database
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            for(ContentValues values : programs) {
                ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI).withValues(values).build());
            }

            try {
                resolver.applyBatch(TvContract.AUTHORITY, ops);
            }
            catch(RemoteException | OperationApplicationException e) {
                Log.e(TAG, "Failed to insert programs.", e);
                return;
            }

            ops.clear();
        }

        Log.i(TAG, "synced schedule for " + existingChannels.size() + " channels");
    }

    private void fetchEPGForChannel(ContentResolver resolver, int uid, long channelId, List<ContentValues> programs) {
        long duration = 60 * 60 * 24 * 2; // EPG duration to fetch (2 days)
        long start = System.currentTimeMillis() / 1000;
        long end = start + duration;

        Uri channelUri = TvContract.buildChannelUri(channelId);

        long last = getLastProgramEndTimeMillis(resolver, channelUri) / 1000;

        if(last > start) {
            start = last;
        }

        // new duration
        duration = end - start;

        if(duration <= 0) {
            return;
        }

        // fetch

        mRequest.createUid();
        mRequest.putU32(uid);
        mRequest.putU32(start);
        mRequest.putU32(duration);

        if(!mConnection.transmitMessage(mRequest, mResponse)) {
            Log.d(TAG, "error sending fetch epg request");
            return;
        }

        mResponse.uncompress();

        // add schedule
        while(!mResponse.eop()) {
            int eventId = (int)mResponse.getU32();
            long startTime = mResponse.getU32();
            long endTime = startTime + mResponse.getU32();
            int content = (int)mResponse.getU32();
            int eventDuration = (int)(endTime - startTime);
            long parentalRating = mResponse.getU32();
            String title = mResponse.getString();
            String plotOutline = mResponse.getString();
            String plot = mResponse.getString();
            String posterUrl = mResponse.getString();
            String backgroundUrl = mResponse.getString();

            // invalid entry
            if(endTime <= startTime) {
                continue;
            }

            Event event = new Event(content, title, plotOutline, plot, eventDuration, eventId, uid);

            ContentValues values = new ContentValues();
            values.put(TvContract.Programs.COLUMN_CHANNEL_ID, channelId);
            values.put(TvContract.Programs.COLUMN_TITLE, event.getTitle());
            values.put(TvContract.Programs.COLUMN_EPISODE_TITLE, plotOutline);
            values.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, plot);
            values.put(TvContract.Programs.COLUMN_LONG_DESCRIPTION, event.getPlot());
            values.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, startTime * 1000);
            values.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, endTime * 1000);
            values.put(TvContract.Programs.COLUMN_CANONICAL_GENRE, mCanonicalGenre.get(event.getContentId()));

            Event.SeasonEpisodeHolder seasonEpisode = event.getSeasionEpisode();
            if(seasonEpisode.valid()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    values.put(TvContract.Programs.COLUMN_SEASON_DISPLAY_NUMBER, seasonEpisode.season);
                    values.put(TvContract.Programs.COLUMN_EPISODE_DISPLAY_NUMBER, seasonEpisode.episode);
                }
                else {
                    values.put(TvContract.Programs.COLUMN_SEASON_NUMBER, seasonEpisode.season);
                    values.put(TvContract.Programs.COLUMN_EPISODE_NUMBER, seasonEpisode.episode);
                }
            }

            // content rating
            if(parentalRating >= 4 && parentalRating <= 18) {
                TvContentRating rating = TvContentRating.createRating("com.android.tv", "DVB", "DVB_" + parentalRating);
                values.put(TvContract.Programs.COLUMN_CONTENT_RATING, rating.flattenToString());
            }

            // store eventId on flags
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                values.put(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG1, eventId);
            }

            // artwork
            if(posterUrl.equals("x")) {
                try {

                    backgroundUrl = "";
                    ArtworkHolder art = mArtwork.fetchForEvent(event);

                    if(art != null) {
                        backgroundUrl = art.getBackgroundUrl();
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }

            // add url (if not empty)
            if(!backgroundUrl.isEmpty()) {
                values.put(TvContract.Programs.COLUMN_POSTER_ART_URI, backgroundUrl);
            }

            // add event
            programs.add(values);
        }
    }

    static void getExistingChannels(ContentResolver resolver, String inputId, SparseArray<Long> existingChannels) {
        // Create a map from original network ID to channel row ID for existing channels.
        existingChannels.clear();

        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        String[] projection = {TvContract.Channels._ID, TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID};

        Cursor cursor = null;

        try {
            cursor = resolver.query(channelsUri, projection, null, null, null);

            while(cursor != null && cursor.moveToNext()) {
                long channelId = cursor.getLong(0);
                int uid = cursor.getInt(1);
                existingChannels.put(uid, channelId);
            }
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    private static long getLastProgramEndTimeMillis(ContentResolver resolver, Uri channelUri) {
        Uri uri = TvContract.buildProgramsUriForChannel(channelUri);
        String[] projection = {TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS};
        Cursor cursor = null;

        try {
            // TvProvider returns programs chronological order by default.
            cursor = resolver.query(uri, projection, null, null, null);

            if(cursor == null || cursor.getCount() == 0) {
                return 0;
            }

            cursor.moveToLast();
            return cursor.getLong(0);
        }
        catch(Exception e) {
            Log.w(TAG, "Unable to get last program end time for " + channelUri, e);
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return 0;
    }

    private static Uri getUriToResource(@NonNull Context context, @AnyRes int resId) throws Resources.NotFoundException {
        /** Return a Resources instance for your application's package. */
        Resources res = context.getResources();
        /**
         * Creates a Uri which parses the given encoded URI string.
         * @param uriString an RFC 2396-compliant, encoded URI
         * @throws NullPointerException if uriString is null
         * @return Uri for this given uri string
         */
        /** return uri */
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                         "://" + res.getResourcePackageName(resId)
                         + '/' + res.getResourceTypeName(resId)
                         + '/' + res.getResourceEntryName(resId));
    }
}
