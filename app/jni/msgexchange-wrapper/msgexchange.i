%module(directors="1", allprotected="1") msgexchange
#pragma SWIG nowarn=503,516,401

#%include "typemaps.i"
#%include "arrays_java.i"
%include "std_string.i"
#%include "std_map.i"
#%include "std_vector.i"
%include "stdint.i"
%include "various.i"

%rename("%(lowercamelcase)s") "";

%rename (Packet) MsgPacket;
%rename (Connection) MsgConnection;
%rename (SessionProxy) MsgSession;
%rename (Ac3Decoder) Ac3Decoder;
%rename (MpegAudioDecoder) MpegAudioDecoder;

%{
#include "msgpacket.h"
#include "msgconnection.h"
#include "msgsession.h"
#include "ac3decoder.h"
#include "mpadecoder.h"
%}


//
// MsgPacket
//

%ignore MsgPacket::read;
%ignore MsgPacket::write;
%ignore MsgPacket::readstream;
%ignore MsgPacket::reserve;
%ignore MsgPacket::consume;
%ignore MsgPacket::put_Blob;
%ignore MsgPacket::get_Blob;
%ignore MsgPacket::getPacket;
%ignore MsgPacket::getPayload;

%include "msgpacket.h"

%extend MsgPacket {

	void skipBuffer(int length) {
		self->consume(length);
	}

	void readBuffer(char* BYTE, int offset, int length) {
		uint8_t* buffer_src = self->consume(length);
		uint8_t* buffer_dst = (uint8_t*)&BYTE[offset];

		memcpy(buffer_dst, buffer_src, length);
	}
}


//
// MsgConnection
//

%feature("director") MsgConnection;
%ignore MsgConnection::pollfd;
%ignore MsgConnection::Reconnect;
%ignore MsgConnection::GetConnectionLost;
%ignore MsgConnection::SetConnectionLost;
%ignore MsgConnection::m_socket;
%ignore MsgConnection::m_timeout;
%newobject MsgConnection::TransmitMessage;

%include "msgconnection.h"

//
// MsgSession
//

%feature("director") MsgSession;
%newobject MsgSession::TransmitMessage;
%ignore MsgSession::Run;

%include "msgsession.h"


//
// AC3Decoder
//

%include "ac3decoder.h"

//
// MpegAudioDecoder
//

%include "mpadecoder.h"

%pragma(java) jniclasscode=%{

static {
	try {
		System.loadLibrary("gnustl_shared");
		System.loadLibrary("msgexchange");
		System.loadLibrary("a52");
		System.loadLibrary("mad");
		System.loadLibrary("msgexchange_wrapper");
	}
	catch (UnsatisfiedLinkError e) {
	}
}
%}
