/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgsession.h
	Header file for the MsgSession class.
	This include file defines the MsgSession class
*/

#ifndef MSGSESSION_H
#define MSGSESSION_H

#include <time.h>
#include <atomic>
#include <map>
#include <mutex>

#include "msgcondition.h"
#include "msgconnection.h"
#include "msgthread.h"

/**
	@short Message client-session class

	Communication client connection class.
	Handles client communication and accepts server notifications.
*/

class MsgSession : public MsgConnection, protected MsgThread {
public:

	/**
	Constructor.
	Creates a MsgSession object
	*/
	MsgSession();

	/**
	Destructor.
	Destroys the MsgSession object
	*/
	virtual ~MsgSession();

	bool Open(const char* hostname, int port);

	/**
	Close the session.
	Shutdown the server session.
	*/
	bool Close();

	/**
	Terminate session.
	Quickly tear down the connection. Acts as if the connection to the server was lost.
	*/
	bool Terminate();

	MsgPacket* TransmitMessage(MsgPacket* message);

	bool TransmitMessage(MsgPacket* request, MsgPacket* response);

protected:

	void Run();

	/**
	Notification handler.
	This virtual member is called on any notification received from the server.
	A notification is a packet that is not a response of a request.

	@param notification pointer to the received notification packet.
	*/
	virtual void OnNotification(MsgPacket* notification);

private:

	/**
		@short Packet Response Condition

		Condition handling response packets.
	*/

	class MsgResponseCondition : public MsgCondition {
	public:

		MsgResponseCondition();

		MsgResponseCondition(MsgPacket* destinationPacket);

		/**
		Signals reponse reception.
		This member is called if a response was received.

		@param p pointer to the response packet.
		*/
		void Signal(MsgPacket* p);

		MsgPacket* packet; /*!< pointer to the response packet, NULL if there wasn't any response received. */

	protected:

		void Signal();

	};

	bool emptyQueue();

	MsgResponseCondition* find(MsgPacket* p);

	std::mutex m_mutex;

	std::map<uint64_t, MsgResponseCondition*> m_queue;
};

/*
@startuml

note top of MsgSession
 <b>Asynchronous Communication Client</b>
 Send a request to the server and
 waits for response message. Other
 messages may be sent from the server to
 the client at any time.

 <b>Sources</b>
 libmsgexchange/include/msglistener.h
 libmsgexchange/src/msglistener.cpp
end note

note left of MsgResponseCondition
 <b>Response Wait Condition</b>
 This class is used for asynchronous
 response signaling.

 <b>Sources</b>
 libmsgexchange/include/msglistener.h
 libmsgexchange/src/msglistener.cpp
end note

MsgSession --|> MsgConnection
MsgSession --|> MsgThread

MsgSession o--> "many" MsgResponseCondition : m_queue
MsgResponseCondition o--> "packet" MsgPacket

class MsgSession {
+bool Open(const char* hostname, int port)
+bool Close()
+MsgPacket* TransmitMessage(MsgPacket* message)
#void Run()
#{abstract}void OnNotification(MsgPacket* notification)
-MsgCondition* find(MsgPacket* p)
-std::mutex m_mutex
-std::map<uint64_t, MsgResponseCondition*> m_queue
}

MsgResponseCondition --|> MsgCondition

class MsgResponseCondition {
+void Signal(MsgPacket* p)

+MsgPacket* packet
}

@enduml
*/

#endif // MSGSESSION_H
