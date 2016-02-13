package org.xvdr.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.Event;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.tv.ChannelList;
import org.xvdr.robotv.tv.ServerConnection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
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

		void onCancel();
	}

	static final String TAG = "ChannelSyncAdapter";

	private Context mContext;
	private ServerConnection mConnection;
    private ArtworkFetcher mArtwork;
	private String mInputId;
	private boolean mCancelChannelSync = false;

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

	public ChannelSyncAdapter(Context context, String inputId, ServerConnection connection) {
		mContext = context;
		mConnection = connection;
		mInputId = inputId;

        mArtwork = new ArtworkFetcher(mConnection, SetupUtils.getLanguage(context));
	}

	public void setProgressCallback(ProgressCallback callback) {
		mProgressCallback = callback;
	}

	public void syncChannels(boolean removeExisting) {
		final SparseArray<Long> existingChannels = new SparseArray<>();
		final ContentResolver resolver = mContext.getContentResolver();

		mCancelChannelSync = false;

		Log.i(TAG, "syncing channel list ...");

		// remove existing channels
		if(removeExisting)  {
			Uri uri = TvContract.buildChannelsUriForInput(mInputId);
			resolver.delete(uri, null, null);
		}

		// fetch existing channel list

		getExistingChannels(mContext, mInputId, existingChannels);

		// update or insert channels

		ChannelList list = new ChannelList();

		list.load(mConnection);

		int i = 0;
        int index = 0;

		for(ChannelList.Entry entry : list) {
			Uri channelUri;
			Long channelId = existingChannels.get(entry.uid);

			// channel entry
			ContentValues values = new ContentValues();
			values.put(TvContract.Channels.COLUMN_INPUT_ID, mInputId);

            values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, Integer.toString(++index));
			values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, entry.name);
			values.put(TvContract.Channels.COLUMN_SERVICE_ID, 0);
			values.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, 0);
			values.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, entry.uid);
			values.put(TvContract.Channels.COLUMN_SERVICE_TYPE, entry.radio ? TvContract.Channels.SERVICE_TYPE_AUDIO : TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO);
			values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_DVB_S2);
			values.put(TvContract.Channels.COLUMN_SEARCHABLE, 1);

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

				if(mCancelChannelSync)  {
					mProgressCallback.onCancel();
				}
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

	public void cancelSyncChannels() {
		mCancelChannelSync = true;
	}

	public void syncChannelIcons() {
		final SparseArray<Long> existingChannels = new SparseArray<>();

		getExistingChannels(mContext, mInputId, existingChannels);

		ChannelList list = new ChannelList();
		list.load(mConnection, new ChannelList.Callback() {
            @Override
            public void onChannel(final ChannelList.Entry entry) {
                Long channelId = existingChannels.get(entry.uid);

                if (channelId == null) {
                    return;
                }

                final Uri uri = TvContract.buildChannelUri(channelId);

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        entry.iconURL = entry.iconURL.replace("darth", "192.168.16.10");
                        fetchChannelLogo(uri, entry.iconURL);
                    }
                });

                t.start();

                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
	}

	public void syncEPG() {
		SparseArray<Long> existingChannels = new SparseArray<>();
		getExistingChannels(mContext, mInputId, existingChannels);

		Log.i(TAG, "syncing epg ...");

		// fetch epg entries for each channel
		int size = existingChannels.size();

		for(int i = 0; i < size; ++i) {
            List<ContentValues> programs = new ArrayList<>();

            fetchEPGForChannel(existingChannels.keyAt(i), existingChannels.valueAt(i), programs);

            // populate database
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            for(ContentValues values : programs) {
                ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI).withValues(values).build());
            }

            try {
                mContext.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
            }
            catch(RemoteException | OperationApplicationException e) {
                Log.e(TAG, "Failed to insert programs.", e);
                return;
            }

            Log.i(TAG, "populated database with " + programs.size() + " entries.");
            ops.clear();
        }

		Log.i(TAG, "synced schedule for " + existingChannels.size() + " channels");
	}

	private void fetchEPGForChannel(int uid, long channelId, List<ContentValues> programs) {
		ContentResolver resolver = mContext.getContentResolver();
		long duration = 60 * 60 * 24 * 2; // EPG duration to fetch (2 days)
		long start = System.currentTimeMillis() / 1000;
		long end = start + duration;

		Uri channelUri = TvContract.buildChannelUri(channelId);

        Log.d(TAG, "feching epg for " + channelUri.toString() + " ...");

		long last = getLastProgramEndTimeMillis(resolver, channelUri) / 1000;

		if(last > start) {
			start = last;
		}

		// new duration
		duration = end - start;

		if(duration <= 0) {
			Log.i(TAG, "duration < 0");
			return;
		}

		// fetch

		Packet req = mConnection.CreatePacket(ServerConnection.XVDR_EPG_GETFORCHANNEL);
		req.putU32(uid);
		req.putU32(start);
		req.putU32(duration);

		Packet resp = mConnection.transmitMessage(req);

		if(resp == null) {
            Log.d(TAG, "error sending fetch epg request");
			return;
		}

		// add schedule
        int i = 0;
		while(!resp.eop()) {
			long eventId = resp.getU32();
			long startTime = resp.getU32();
			long endTime = startTime + resp.getU32();
			int content = (int)resp.getU32();
            int eventDuration = (int)(endTime - startTime);
			long parentalRating = resp.getU32();
			String title = resp.getString();
			String plotOutline = resp.getString();
			String plot = resp.getString();
            String posterUrl = resp.getString();
            String backgroundUrl = resp.getString();

            String description = plotOutline.trim();
            if(!description.isEmpty() && !plot.isEmpty()) {
                description += " - ";
            }

            description += plot;

            Event event = new Event(content, title, plotOutline, plot, eventDuration);

            ContentValues values = new ContentValues();
            values.put(TvContract.Programs.COLUMN_CHANNEL_ID, channelId);
            values.put(TvContract.Programs.COLUMN_TITLE, event.getTitle());
            values.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, description.substring(0, Math.min(description.length(), 400)));
            values.put(TvContract.Programs.COLUMN_LONG_DESCRIPTION, event.getPlot());
            values.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, startTime * 1000);
            values.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, endTime * 1000);
            values.put(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_DATA, eventId);
            values.put(TvContract.Programs.COLUMN_CANONICAL_GENRE, mCanonicalGenre.get(event.getContentId()));

            // content rating
            if(parentalRating >= 4 && parentalRating <= 18) {
                TvContentRating rating = TvContentRating.createRating("com.android.tv", "DVB", "DVB_" + parentalRating);
                values.put(TvContract.Programs.COLUMN_CONTENT_RATING, rating.flattenToString());
            }

            // artwork
            if (posterUrl.equals("x")) {
                try {

                    backgroundUrl = "";
                    ArtworkHolder art = mArtwork.fetchForEvent(event);

                    if (art != null) {
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
            i++;
		}

        Log.d(TAG, "synced " + i + " epg events");
	}

	private void fetchChannelLogo(Uri channelUri, String address) {
		URL sourceUrl;
		OutputStream os;
		InputStream in;
		URLConnection urlConnection;
		Uri channelLogoUri = TvContract.buildChannelLogoUri(channelUri);

		try {
			os = mContext.getContentResolver().openOutputStream(channelLogoUri);
			sourceUrl = new URL(address);
			urlConnection = sourceUrl.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream());
		}
		catch(Exception e) {
			return;
		}

		byte[] buffer = new byte[32768];
		int bytes_read;

		try {
			while((bytes_read = in.read(buffer)) > 0) {
				os.write(buffer, 0, bytes_read);
			}

			in.close();
			os.close();
		}
		catch(IOException e) {
		}

	}

	public static void getExistingChannels(Context context, String inputId, SparseArray<Long> existingChannels) {
		// Create a map from original network ID to channel row ID for existing channels.
		existingChannels.clear();

		Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
		String[] projection = {TvContract.Channels._ID, TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID};

		Cursor cursor = null;
		ContentResolver resolver = context.getContentResolver();

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

	public static long getLastProgramEndTimeMillis(ContentResolver resolver, Uri channelUri) {
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
}
