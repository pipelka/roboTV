package org.xvdr.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.example.android.sampletvinput.setup.SetupUtils;

import org.xvdr.msgexchange.Packet;
import org.xvdr.tv.ChannelList;
import org.xvdr.tv.ServerConnection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by pipelka on 05.05.15.
 */
public class ChannelSyncAdapter {

	public interface ProgressCallback {

		void onProgress(int done, int total);

		void onDone();

		void onCancel();
	}

	static final String TAG = "ChannelSyncAdapter";
	private static final int BATCH_OPERATION_COUNT = 100;

	private Context mContext;
	private ServerConnection mConnection;
	private String mInputId;
	private ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(10);
	private boolean mCancelChannelSync = false;
	private static final SparseArray<String> mBroadcastGenre = new SparseArray<String>() {
		{
			append(0x10, "Movie,Drama");
			append(0x20, "News,Current,Affairs");
			append(0x30, "Show");
			append(0x40, "Sports");
			append(0x50, "Children,Youth");
			append(0x60, "Music,Ballet,Dance");
			append(0x70, "Arts,Culture");
			append(0x80, "Social,Political,Economics");
			append(0x90, "Educational,Science");
			append(0xa0, "Leisure,Hobbies");
			append(0xb0, "Special");
		}
	};

	private static final SparseArray<String> mCanonicalGenre = new SparseArray<String>() {
		{
			append(0x10, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MOVIES));
			append(0x20, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.NEWS));
			append(0x30, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ENTERTAINMENT));
			append(0x40, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.SPORTS));
			append(0x50, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.FAMILY_KIDS));
			append(0x60, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.MUSIC));
			append(0x70, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.ARTS));
			append(0x80, "");
			append(0x90, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.EDUCATION));
			append(0xa0, TvContract.Programs.Genres.encode(TvContract.Programs.Genres.LIFE_STYLE));
			append(0xb0, "");
		}
	};

	private ProgressCallback mProgressCallback = null;

	public ChannelSyncAdapter(Context context, String inputId, ServerConnection connection) {
		mContext = context;
		mConnection = connection;
		mInputId = inputId;
	}

	public void SetProgressCallback(ProgressCallback callback) {
		mProgressCallback = callback;
	}

	public void SyncChannels(boolean removeExisting) {
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
			//values.put(TvContract.Channels.COLUMN_DESCRIPTION, "HD");

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

	public void SyncChannelIcons() {
		final SparseArray<Long> existingChannels = new SparseArray<>();

		getExistingChannels(mContext, mInputId, existingChannels);

		ChannelList list = new ChannelList();
		list.load(mConnection, new ChannelList.Callback() {
			@Override
			public void onChannel(final ChannelList.Entry entry) {
				Long channelId = existingChannels.get(entry.uid);

				if(channelId == null) {
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
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}

			}
		});
	}

	public void SyncEPG() {
		SparseArray<Long> existingChannels = new SparseArray<>();
		getExistingChannels(mContext, mInputId, existingChannels);

		Log.i(TAG, "syncing epg ...");

		List<ContentValues> programs = new ArrayList<>();

		// fetch epg entries for each channel
		int size = existingChannels.size();

		for(int i = 0; i < size; ++i) {
			FetchEPGForChannel(existingChannels.keyAt(i), existingChannels.valueAt(i), programs);
		}

		Log.i(TAG, "populating database with " + programs.size() + " entries ...");

		// populate database
		ArrayList<ContentProviderOperation> ops = new ArrayList<>();

		for(ContentValues values : programs) {
			ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI).withValues(values).build());

			if(ops.size() == BATCH_OPERATION_COUNT) {
				try {
					mContext.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
				}
				catch(RemoteException | OperationApplicationException e) {
					Log.e(TAG, "Failed to insert programs.", e);
					return;
				}

				ops.clear();
			}
		}

		// commit last part
		try {
			mContext.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
		}
		catch(RemoteException | OperationApplicationException e) {
			Log.e(TAG, "Failed to insert programs.", e);
			return;
		}

		Log.i(TAG, "synced schedule for " + existingChannels.size() + " channels");
	}

	private void FetchEPGForChannel(int uid, long channelId, List<ContentValues> programs) {
		ContentResolver resolver = mContext.getContentResolver();
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
			return;
		}

		// add schedule

		while(!resp.eop()) {
			long eventId = resp.getU32();
			long startTime = resp.getU32();
			long endTime = startTime + resp.getU32();
			long content = resp.getU32();
			int genreType = (int)content & 0xF0;
			long genreSubType = content & 0x0F;
			long parentalRating = resp.getU32();
			String title = resp.getString();
			String plotOutline = resp.getString();
			String plot = resp.getString();

			ContentValues values = new ContentValues();
			values.put(TvContract.Programs.COLUMN_CHANNEL_ID, channelId);
			values.put(TvContract.Programs.COLUMN_TITLE, title);
			values.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, plotOutline);
			values.put(TvContract.Programs.COLUMN_LONG_DESCRIPTION, plot);
			values.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, startTime * 1000);
			values.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, endTime * 1000);
			values.put(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_DATA, eventId);
			values.put(TvContract.Programs.COLUMN_BROADCAST_GENRE, mBroadcastGenre.get(genreType));
			values.put(TvContract.Programs.COLUMN_CANONICAL_GENRE, mCanonicalGenre.get(genreType));
			programs.add(values);
		}
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
