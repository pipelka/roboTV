package org.xvdr.robotv.tv;

import org.xvdr.msgexchange.Packet;

import java.util.ArrayList;

/**
 * Created by pipelka on 04.05.15.
 */
public class ChannelList extends ArrayList<ChannelList.Entry> {

	public interface Callback {
		void onChannel(Entry entry);
	}

	public class Entry {
		public long number = 0;
		public String name;
		public int uid = 0;
		public int caid = 0;
		public String iconURL;
		public String serviceReference;
		public boolean radio = false;

	}

	public void load(ServerConnection connection) {
		clear();

		loadChannelType(connection, false, null);
	}

	public void load(ServerConnection connection, Callback callback) {
		clear();

		loadChannelType(connection, false, callback);
	}

	private boolean loadChannelType(ServerConnection connection, boolean radio, Callback callback) {
		Packet req = connection.CreatePacket(ServerConnection.XVDR_CHANNELS_GETCHANNELS);
		req.putU32(radio ? 1 : 2);

		Packet resp = connection.transmitMessage(req);

		if(resp == null) {
			return false;
		}

		while(!resp.eop()) {
			Entry e = new Entry();
			e.number = resp.getU32();
			e.name = resp.getString();
			e.uid = (int) resp.getU32();
			e.caid = (int) resp.getU32();
			e.iconURL = resp.getString();
			e.serviceReference = resp.getString();
			e.radio = radio;

			if(callback != null) {
				callback.onChannel(e);
			}
			else {
				add(e);
			}
		}

		return true;
	}
}
