/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgcallbacks.h
	Header file for the MsgCallbacks class.
	This include file defines the MsgCallbacks class
*/

#ifndef MSGCALLBACKS_H
#define MSGCALLBACKS_H

#include "msghandler.h"
#include <map>

/**
	@short Message function callback class

	Registers and calls member functions for received messages
*/

template<class T, class P = MsgHandler>
class MsgCallbacks : public P {
public:

	/**
	Constructor.
	Creates a MsgCallbacks object
	*/
	MsgCallbacks(int sock) : P(sock) {}

	MsgCallbacks() {}

protected:

	typedef	bool(T::*MSGFPTR)(MsgPacket* request, MsgPacket* response);
	typedef std::map<uint16_t, MSGFPTR> MSGHANDLERS;

	typedef	void(T::*NTFFPTR)(MsgPacket* notification);
	typedef std::map<uint16_t, NTFFPTR> NTFHANDLERS;

	/**
	Register a message handler
	Registers a class member function for a given message id

	@param msgid id of the message
	@param handler reference to the member function
	*/
	void RegisterMessageHandler(uint16_t msgid, MSGFPTR handler) {
		m_msg[msgid] = handler;
	}

	/**
	Register a notification handler
	Registers a class member function for a given notification (asynchronous message from server to client)

	@param msgid id of the message
	@param handler reference to the member function
	*/
	void RegisterNotificationHandler(uint16_t msgid, NTFFPTR handler) {
		m_ntf[msgid] = handler;
	}

	/**
	Check and call registered message handlers
	Calls a registered handler for a message

	@param request pointer to incoming message
	@param response pointer to message response
	@return true request has benn successfully processed and the response should be sent
	*/
	bool OnMessage(MsgPacket* request, MsgPacket* response) {
		// check for a registered message handler
		typename MSGHANDLERS::iterator i = m_msg.find(request->getMsgID());

		if(i != m_msg.end()) {
			MSGFPTR func = i->second;
			return (static_cast<T*>(this)->*func)(request, response);
		}

		return false;
	}

	/**
	Check and call registered notification handlers
	Calls a registered handler for a notification

	@param notification pointer to notification message
	*/
	void OnNotification(MsgPacket* notification) {
		// check for a notification message handler
		typename NTFHANDLERS::iterator i = m_ntf.find(notification->getMsgID());

		if(i != m_ntf.end()) {
			NTFFPTR func = i->second;
			(static_cast<T*>(this)->*func)(notification);
		}
	}

private:

	MSGHANDLERS m_msg;

	NTFHANDLERS m_ntf;

};

/*
@startuml

note top of MsgCallbacks
 <b>Function Callback Handler</b>
 Registers function callbacks for
 message ids.

 <b>Sources</b>
 libmsgexchange/include/msgcallbacks.h
end note

MsgCallbacks --|> MsgHandler

class MsgCallbacks<ClientHandlerClass>
class MsgCallbacks {
#void RegisterMessageHandler(uint16_t msgid, MSGFPTR handler)
#void RegisterNotificationHandler(uint16_t msgid, NTFFPTR handler)
#bool OnMessage(MsgPacket* request, MsgPacket* response)
#void OnNotification(MsgPacket* notification)
-MSGHANDLERS m_msg
-NTFHANDLERS m_ntf
}
@enduml
*/

#endif // MSGCALLBACKS_H
