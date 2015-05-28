/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msghandler.h
	Header file for the MsgHandler class.
	This include file defines the MsgHandler class
*/

#ifndef MSGHANDLER_H
#define MSGHANDLER_H

#include "msgpacket.h"
#include "msghandlerbase.h"

#include <queue>
#include <mutex>

/**
	@short Message Handler Class

	This class handles a client connection.
*/

class MsgHandler: public MsgHandlerBase {
public:

	/**
	Constructor.
	Constructs a MsgHandler object.

	@param sock file-descriptor of the client-socket
	 */
	MsgHandler(int sock);

	/**
	Destructor.
	Destroys the MsgHandler object.
	*/
	virtual ~MsgHandler();

	/**
	Send notification
	Sends a notification message to the client

	@param packet pointer to notification message packet
	@return true on success
	*/
	bool QueueNotification(MsgPacket* packet);

	/**
	Set size of the notification queue.
	Defines the maximum number of entries that can be buffered by the notification queue.

	@param queuesize maximum number of notification (default 50)
	@return true on success
	*/
	void SetQueueSize(int queuesize);

	MsgHandlerBase* Clone(int fd);

protected:

	/**
	Event-loop of the message-handler.
	Thread handling the client-connection.
	*/
	void Run();

	/**
	Packet handler.
	This virtual member must be overridden by specific client-handlers.

	@param request pointer to incoming message packet.
	@param response pointer to outgoing response packet. this packet must by filled by user-implementations.
	@return return true if the packet was processed successfully
	*/
	virtual bool OnMessage(MsgPacket* request, MsgPacket* response);

private:

	std::queue<MsgPacket*> m_queue;

	int m_queuesize;

	std::mutex m_mutex;

};

/*
@startuml

note top of MsgHandler
 <b>Connection Handler</b>
 Handles the communcation for a socket
 connection.

 <b>Sources</b>
 libmsgexchange/include/msghandler.h
 libmsgexchange/src/msghandler.cpp
end note

MsgHandler --|> MsgHandlerBase

class MsgHandler {
#void Run()
#{abstract}bool OnMessage(MsgPacket* request, MsgPacket* response)
#bool SendNotification(MsgPacket* packet)
-std::mutex m_mutex
}

@enduml
*/
#endif // MSGHANDLER_H
