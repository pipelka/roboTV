%module(directors="1") jniwrap

%include "std_string.i"
%include "stdint.i"
%include "various.i"

%rename("%(lowercamelcase)s") "";

%rename (Packet) MsgPacket;
%rename (Connection) MsgConnection;
%rename (SessionProxy) MsgSession;
%rename (SessionListener) SessionListener;
%rename (Session) Session;

%{
#include "msgpacket.h"
#include "msgconnection.h"
#include "msgsession.h"
#include "sessionlistener.h"
#include "session.h"
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

	void writeBuffer(char* BYTE, int offset, int length) {
		uint8_t* buffer_src = (uint8_t*)&BYTE[offset];
		self->put_Blob(buffer_src, length);
	}
}


//
// MsgConnection
//

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

%newobject MsgSession::TransmitMessage;
%ignore MsgSession::Run;

%include "msgsession.h"

//
// SessionListener
//

%feature("director") SessionListener;
%include "sessionlistener.h"

//
// Session
//

%include "session.h"

%pragma(java) jniclasscode=%{

static {
	try {
		System.loadLibrary("a52");
		System.loadLibrary("mad");
		System.loadLibrary("msgexchange");
		System.loadLibrary("jniwrap");
	}
	catch (UnsatisfiedLinkError e) {
		e.printStackTrace();
	}
}
%}
