/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgconnection.h
	Header file for the MsgConnection class.
	This include file defines the MsgConnection class
*/

#ifndef MSGCONNECTION_H
#define MSGCONNECTION_H

#include <string>
#include <atomic>

class MsgPacket;

/**
	@short Message Connection Class

	This class provides the client connection to the messageserver.
	All message packets are transmitted via this class.
*/

class MsgConnection {
public:

	/**
	MsgConnection constructor.
	Creates a connection object
	*/
	MsgConnection();

	/**
	MsgConnection destructor.
	Destroys a connection object. Connections must be closed before deleting them.
	*/
	virtual ~MsgConnection();

	/**
	Open a connection.
	Tries to connect to a running message server.

	@param hostname  IP-Address or hostname of the server
	@param port      TCP/IP port the server is listening on
	@return true on success / false if the server is unreachable
	*/
	bool Open(const char* hostname, int port);

	/**
	Abort a connection.
	Immediately abort the connection without closing it. Packet transport will be
	impossible after aborting the connection.
	*/
	void Abort();

	/**
	Close a connection.
	Disconnect from the server.
	*/
	bool Close();

	/**
	Check if connection is open.
	Check if the connection is in an open state

	@return true if the connection is open / false if it's closed or hasn't been opened yet.
	*/
	bool IsOpen() const;

	/**
	Check if connection if aborted.
	Check if the connectio in in an aborted state

	@return true if the connection was aborted.
	*/
	bool IsAborting() const;

	/**
	Send request to server.
	Send a message packet to the server

	@param request Pointer to message packet
	@return true if the packet was sucessfully transmitted / false on failure
	*/
	bool SendRequest(MsgPacket* request);

	/**
	Read response from server.
	Wait for a response packet from the server.

	@return Pointer to the received message packet or NULL if there hasn't been any packet received
	*/
	MsgPacket* ReadResponse();

	bool ReadResponse(MsgPacket* p);

	/**
	Transmit message to server.
	Sends a message to the server and receives the response from the server.

	@param message Pointer to message packet
	@return Pointer to the received message packet or NULL if there hasn't been any packet received
	*/
	virtual MsgPacket* TransmitMessage(MsgPacket* message);

	virtual bool TransmitMessage(MsgPacket* request, MsgPacket* response);

	/**
	Set transmit timeout.
	Defines the amount the time needed to transfer messages to the server and the amount of time the server must response within.
	@param timeout_ms Timeout value in milliseconds
	*/
	void SetTimeout(int timeout_ms);

	/**
	Check if we can send / receive data on a socket
	@param fd filedescriptor of socket
	@param timeout_ms time to wait until socket becomes ready (in milliseconds)
	@param in true - check for incoming data / false - check if socket is ready to send
	@return true on success / false on timeout
	*/
	static bool pollfd(int fd, int timeout_ms, bool in);

	/**
	Get the name of the host we are connected to.
	This is the same as specified at Open()
	@return name of the host
	*/
	const std::string& GetHostname();

protected:

	/**
	Connection lost handler.
	This virtual member is called if the connection to the server has been lost.
	*/
	virtual void OnDisconnect();

	/**
	Connection restored handler.
	This virtual member is called if the connection to the server has been restored.
	*/
	virtual void OnReconnect();

	/**
	Reestablish a lost connection.
	Tries to reconnect to the server

	@return true on success
	*/
	bool Reconnect();

	/**
	Mark a connection as "lost".
	*/
	void SetConnectionLost();

	/**
	Get connection status.
	@return true if the connection was lost
	*/
	bool GetConnectionLost() const;

	int m_socket;

	int m_timeout;

private:

	bool m_connectionlost;

	std::string m_hostname;

	int m_port;

	std::atomic<bool> m_aborting;
};

/*
@startuml

note top of MsgConnection
 <b>Synchronous Communication Client</b>
 Send a request to the server and
 expects a synchronous message response

 <b>Sources</b>
 libmsgexchange/include/msgconnection.h
 libmsgexchange/src/msgconnection.cpp
end note

class MsgConnection {
+bool Open(const char* hostname, int port)
+void Abort()
+void Close()
+bool IsOpen()
+bool IsAborting()
+bool SendRequest(MsgPacket* request)
+MsgPacket* ReadResponse()
+MsgPacket* TransmitMessage(MsgPacket* message)
+void SetTimeout(int timeout_ms)
+{static}bool pollfd(int fd, int timeout_ms, bool in)
#const std::string& GetHostname()
#{abstract}void OnDisconnect()
#{abstract}void OnReconnect()
#bool Reconnect()
#void SetConnectionLost()
#bool GetConnectionLost()
#int m_socket
#int m_timeout
-bool m_connectionlost
-std::string m_hostname
-int m_port
-bool m_aborting
}

@enduml
*/

#endif // MSGCONNECTION_H
