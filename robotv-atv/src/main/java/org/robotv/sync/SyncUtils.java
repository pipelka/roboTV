package org.robotv.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import org.robotv.msgexchange.Packet;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.Connection;
import org.robotv.client.PacketAdapter;
import org.robotv.client.model.Event;

import java.io.IOException;
import java.util.List;

import androidx.tvprovider.media.tv.TvContractCompat;

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

public class SyncUtils {

    static private final String TAG = SyncUtils.class.getName();

    public static final SparseArray<String> canonicalGenre = new SparseArray<String>() {
        {
            append(0x10, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
            append(0x11, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
            append(0x12, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
            append(0x13, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
            append(0x14, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.COMEDY));
            append(0x15, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ENTERTAINMENT));
            append(0x16, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
            append(0x17, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.DRAMA));
            append(0x20, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.NEWS));
            append(0x21, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.NEWS));
            append(0x22, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.NEWS));
            append(0x23, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.TECH_SCIENCE));
            append(0x30, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ENTERTAINMENT));
            append(0x31, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ENTERTAINMENT));
            append(0x32, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ENTERTAINMENT));
            append(0x33, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ENTERTAINMENT));
            append(0x40, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x41, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x42, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x43, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x44, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x45, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x46, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x47, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x48, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x49, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x4A, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x4B, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SPORTS));
            append(0x50, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.FAMILY_KIDS));
            append(0x51, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.EDUCATION));
            append(0x52, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.FAMILY_KIDS));
            append(0x53, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.FAMILY_KIDS));
            append(0x54, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.EDUCATION));
            append(0x53, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.FAMILY_KIDS));
            append(0x60, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MUSIC));
            append(0x61, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MUSIC));
            append(0x62, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MUSIC));
            append(0x63, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MUSIC));
            append(0x64, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MUSIC));
            append(0x70, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ARTS));
            append(0x71, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ARTS));
            append(0x72, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ARTS));
            append(0x74, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ARTS));
            append(0x75, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ARTS));
            append(0x76, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
            append(0x77, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
            append(0x78, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.NEWS));
            append(0x79, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.NEWS));
            append(0x7A, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ARTS));
            append(0x81, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.TECH_SCIENCE));
            append(0x90, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.TECH_SCIENCE));
            //append(0x91, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ANIMAL_WILDLIFE));
            append(0x92, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.TECH_SCIENCE));
            append(0x93, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.TECH_SCIENCE));
            append(0x94, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.TRAVEL));
            append(0xA0, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.LIFE_STYLE));
            append(0xA1, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.TRAVEL));
            append(0xA2, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.ARTS));
            append(0xA3, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.LIFE_STYLE));
            append(0xA4, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.LIFE_STYLE));
            append(0xA5, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.LIFE_STYLE));
            append(0xA6, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.SHOPPING));
            append(0xA7, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.LIFE_STYLE));
            append(0xF0, TvContractCompat.Programs.Genres.encode(TvContractCompat.Programs.Genres.MOVIES));
        }
    };

    public static class ChannelHolder {
        public Uri channelUri;
        public long channelId = 0;
        public int channelUid = 0;
        public int displayNumber = 0;
        public String displayName;
    }

    static public boolean getChannelInfo(ContentResolver resolver, Uri channelUri, ChannelHolder holder) {
        if(channelUri == null) {
            return false;
        }

        String[] projection = {
                TvContractCompat.Channels._ID,
                TvContractCompat.Channels.COLUMN_ORIGINAL_NETWORK_ID,
                TvContractCompat.Channels.COLUMN_DISPLAY_NUMBER,
                TvContractCompat.Channels.COLUMN_DISPLAY_NAME
        };

        try (Cursor cursor = resolver.query(channelUri, projection, null, null, null)) {

            while (cursor != null && cursor.moveToNext()) {
                holder.channelId = cursor.getLong(0);
                holder.channelUid = cursor.getInt(1);
                holder.displayNumber = cursor.getInt(2);
                holder.displayName = cursor.getString(3);
                holder.channelUri = channelUri;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static long getLastProgramEndTimeMillis(ContentResolver resolver, Uri channelUri) {
        Uri uri = TvContractCompat.buildProgramsUriForChannel(channelUri);
        String[] projection = {TvContractCompat.Programs.COLUMN_END_TIME_UTC_MILLIS};

        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
            // TvProvider returns programs chronological order by default.

            if (cursor == null || cursor.getCount() == 0) {
                return 0;
            }

            cursor.moveToLast();
            return cursor.getLong(0);
        } catch (Exception e) {
            Log.w(TAG, "Unable to get last program end time for " + channelUri, e);
        }

        return 0;
    }

    static boolean fetchEPGForChannel(Connection connection, String language, ContentResolver resolver, Uri channelUri, List<ContentValues> programs, boolean appendEntries) {
        if(connection == null || !connection.isOpen()) {
            return false;
        }

        long duration = 60 * 60 * 24 * 2; // EPG duration to fetch (2 days)
        long start = System.currentTimeMillis() / 1000;
        long end = start + duration;

        ChannelHolder holder = new ChannelHolder();

        if(!SyncUtils.getChannelInfo(resolver, channelUri, holder)) {
            return false;
        }

        Log.d(TAG, String.format("Fetching %s EPG entries for channel %d - %s", (appendEntries ? "new" : "all"), holder.displayNumber, holder.displayName));

        long last = appendEntries ? getLastProgramEndTimeMillis(resolver, holder.channelUri) / 1000 : 0;

        if(last > start) {
            start = last;
        }

        // new duration
        duration = end - start;

        if(duration <= 0) {
            return true;
        }

        // fetch

        Packet request = connection.CreatePacket(Connection.EPG_GETFORCHANNEL);
        request.putU32(holder.channelUid);
        request.putU32(start);
        request.putU32(duration);

        Packet response = new Packet();

        if(!connection.transmitMessage(request, response)) {
            Log.d(TAG, "error sending fetch epg request");
            return false;
        }

        response.uncompress();

        // add schedule
        while(!response.eop()) {
            Event event = PacketAdapter.toEpgEvent(response);
            event.setChannelUid(holder.channelUid);

            // invalid entry
            if(event.getDuration() <= 0) {
                continue;
            }

            ContentValues values = new ContentValues();
            values.put(TvContractCompat.Programs.COLUMN_CHANNEL_ID, holder.channelId);
            values.put(TvContractCompat.Programs.COLUMN_TITLE, event.getTitle());

            if(!TextUtils.isEmpty(event.getShortText())) {
                values.put(TvContractCompat.Programs.COLUMN_EPISODE_TITLE, event.getShortText());
            }

            String description = event.getDescription();
            if(!TextUtils.isEmpty(description)) {
                if(description.length() <= 256) {
                    values.put(TvContractCompat.Programs.COLUMN_SHORT_DESCRIPTION, description);
                }
                else {
                    values.put(TvContractCompat.Programs.COLUMN_SHORT_DESCRIPTION, description.substring(0, 256) + "...");
                }
            }

            values.put(TvContractCompat.Programs.COLUMN_START_TIME_UTC_MILLIS, event.getStartTime() * 1000);
            values.put(TvContractCompat.Programs.COLUMN_END_TIME_UTC_MILLIS, event.getEndTime() * 1000);
            values.put(TvContractCompat.Programs.COLUMN_CANONICAL_GENRE, canonicalGenre.get(event.getContentId()));

            Event.SeasonEpisodeHolder seasonEpisode = event.getSeasionEpisode();

            if(seasonEpisode.valid()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    values.put(TvContractCompat.Programs.COLUMN_SEASON_DISPLAY_NUMBER, seasonEpisode.season);
                    values.put(TvContractCompat.Programs.COLUMN_EPISODE_DISPLAY_NUMBER, seasonEpisode.episode);
                }
                else {
                    values.put(TvContractCompat.Programs.COLUMN_SEASON_NUMBER, seasonEpisode.season);
                    values.put(TvContractCompat.Programs.COLUMN_EPISODE_NUMBER, seasonEpisode.episode);
                }
            }

            // content rating
            long parentalRating = event.getParentalRating();

            if(parentalRating >= 4 && parentalRating <= 18) {
                TvContentRating rating = TvContentRating.createRating("com.android.tv", "DVB", "DVB_" + parentalRating);
                values.put(TvContractCompat.Programs.COLUMN_CONTENT_RATING, rating.flattenToString());
            }

            // store eventId in FLAG1
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                values.put(TvContractCompat.Programs.COLUMN_INTERNAL_PROVIDER_FLAG1, event.getEventId());
            }

            // store VPS time in FLAG2
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                values.put(TvContractCompat.Programs.COLUMN_INTERNAL_PROVIDER_FLAG2, event.getVpsTime());
            }

            // artwork
            ArtworkFetcher artwork = new ArtworkFetcher(connection, language);

            try {
                if(artwork.fetchForEvent(event)) {
                    String url = event.getBackgroundUrl();
                    values.put(TvContractCompat.Programs.COLUMN_POSTER_ART_URI, (!TextUtils.isEmpty(url) && !url.equals("x")) ? url : "");
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }

            // add event
            programs.add(values);
        }

        return true;
    }

}
