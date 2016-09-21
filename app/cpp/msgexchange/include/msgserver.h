/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgserver.h
	Header file for the MsgServer class.
	This include file defines the MsgServer class
*/

#ifndef MSGSERVER_H
#define MSGSERVER_H

#include <list>

#include "msgthread.h"
#include "msgthreadqueue.h"
#include <mutex>

class MsgHandlerBase;

/**
	@short Message Server Class

	This class provides the message-server.
	Clients (MsgConnection, MsgListener) objects may connect to this server for message exchange.
*/

class MsgServer : public MsgThread {
public:

	/**
	MsgServer constructor.
	Creates a message-server object listening of a specific port. Every connected client is handled
	by a MsgHandler object.

	@param	port TCP/IP port the server is listening on
	*/
	MsgServer(int port);

	/**
	Destructor.
	Destroys the MsgServer object.
	*/
	virtual ~MsgServer();

	/**
	Start message server.
	Start the message server and waits for incoming connections.
	*/
	bool Start();

	/**
	Stop server.
	Stops the message server.
	*/
	bool Stop();

	/**
	Set TCP/IP port.
	Change the port the server is listening on
	@param port TCP/IP port
	@return true on success (server must neither be running nor clients connected)
	*/
	bool SetPort(int port);

	enum PersistanceMethod {
		PERSIST_CONNECTION_NONE,
		PERSIST_CONNECTION_IP
	};

	/**
	Set client connection persistance method.
	Changed the method used for persisting client connection. Default is PERSIST_CONNECTION_NONE.
	@param method persistance method
	*/
	void SetPersistance(enum PersistanceMethod method);

protected:

	/**
	Run server thread.
	Main event loop of the server.
	*/
	void Run();

	int CreateServerSocket();

	/**
	Create a client handler.
	Creates a new client handler object serving a connection. This virtual member may be overridden to create specific
	client-handlers.

	@param sock file-descriptor of the client-socket.
	@return pointer to the newly created MsgHandler object
	*/
	virtual MsgHandlerBase* CreateHandler(int sock) = 0;

	void foreach(std::function<void(MsgThread*)> fn);

private:

	MsgHandlerBase* FindHandlerByID(uint64_t id);

	int m_port;

	int m_server;

	std::mutex m_mutex;

	MsgThreadQueue m_handlers;

	enum PersistanceMethod m_persistance;

};

/*
@startuml

note top of MsgServer
 <b>Communication Server</b>
 Waits for incoming connections and
 creates a message handlers for every
 connection.

 <b>Sources</b>
 libmsgexchange/include/msgserver.h
 libmsgexchange/src/msgserver.cpp
end note

MsgServer --|> MsgThread
MsgServer o--> "many" MsgHandlerBase : m_handlers

enum PersistanceMethod {
PERSIST_CONNECTION_NONE,
PERSIST_CONNECTION_IP
}

MsgServer *--> PersistanceMethod : m_persistance

class MsgServer {
+bool Start()
+bool Stop()
#void Run()
#void Cleanup()
#int CreateServerSocket()
#{abstract}MsgHandlerBase* CreateHandler(int sock)
-int m_port
-int m_server
-std::mutex m_mutex
-std::list<MsgHandler*> m_handlers
-PersistanceMethod m_persistance
}

@enduml
*/

#endif // MSGSERVER_H
