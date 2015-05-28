/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msghandlerbase.h
	Header file for the MsgHandlerBase class.
	This include file defines the MsgHandlerBase class
*/

#ifndef MSGHANDLERBASE_H
#define MSGHANDLERBASE_H

#include "msgthread.h"
#include "msgserver.h"
#include <atomic>
#include <mutex>
#include <stdint.h>

/**
	@short Message Handler Class

	This class handles a client connection.
*/

class MsgHandlerBase : public MsgThread {
public:

	/**
	Constructor.
	Constructs a MsgHandler object.

	@param sock file-descriptor of the client-socket
	 */
	MsgHandlerBase(int sock);

	/**
	Destructor.
	Destroys the MsgHandler object.
	*/
	virtual ~MsgHandlerBase();

	virtual bool Start();

	/**
	Stop the client-handler.
	Try to shutdown the client-handler.

	@return true on success
	*/
	virtual bool Stop();

	/**
	Set transmit timeout.
	Defines the amount the time needed to transfer messages to the client
	@param timeout_ms Timeout value in milliseconds
	*/
	void SetTimeout(int timeout_ms);

	/**
	Get connection persistance
	Defines if a client wants to persist the handler objects during connection loss / restore
	@return true if the clienthandler should be persisted
	*/
	enum MsgServer::PersistanceMethod GetPersistance() const;

	void SetPersistance(enum MsgServer::PersistanceMethod method);

	/**
	Set a unique client id for this connection handler.
	@param id the unique client id
	*/
	void SetClientID(uint64_t id);

	/**
	Get the unique client id for this connection handler.
	@return the unique client id
	*/
	uint64_t GetClientID() const;

	bool IsDisconnected() const;

	virtual MsgHandlerBase* Clone(int fd) = 0;

protected:

	/**
	Set the socket descriptor.
	Changes the socket descriptor of the handler (used for persistent connections)
	@param sock the new descriptor
	*/
	void SetSocket(int sock);

	/**
	Signal client reconnection.
	*/
	virtual void OnClientReconnect();

	void Lock();

	bool TryLock();

	void Unlock();

	friend class MsgServer;

	std::atomic<int> m_sock;

	std::atomic<bool> m_disconnected;

	int m_timeout;

	enum MsgServer::PersistanceMethod m_persistance;

private:

	std::mutex m_mutex;

	std::mutex m_lock;
};

/*
@startuml

note top of MsgHandlerBase
 <b>Connection Handler Base</b>
 Handles the a socket connection.

 <b>Sources</b>
 libmsgexchange/include/msghandlerbase.h
 libmsgexchange/src/msghandlerbase.cpp
end note

MsgHandlerBase --|> MsgThread
MsgHandlerBase *--> PersistanceMethod : m_persistance

class MsgHandlerBase {
+MsgHandlerBase(int sock)
+{abstract}~MsgHandlerBase()
+bool Stop()
+void SetTimeout(int timeout_ms)
+bool GetPersist()
+bool SetClientID(uint64_t id)
+uint64_t GetClientID()
#void SetSocket(int sock)
#int m_sock
#int m_timeout
#bool m_persist
#PersistanceMethod m_persistance
#uint64_t m_clientid
}

@enduml
*/
#endif // MSGHANDLERBASE_H
