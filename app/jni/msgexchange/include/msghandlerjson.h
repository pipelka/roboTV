/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msghandlerjson.h
	Header file for the JSON message handler class.
	This include file defines the MsgHandlerJSON class
*/

#ifndef MSGHANDLERJSON_H
#define MSGHANDLERJSON_H

#include "msghandler.h"

/**
	@short JSON Message Handler Class

	This class handles JSON client messages.
*/

class MsgHandlerJSON: public MsgHandler {
public:

	/**
	Constructor.
	Constructs a MsgHandlerJSON object.

	@param sock file-descriptor of the client-socket
	 */
	MsgHandlerJSON(int sock);

	void SetMessageType(int msgtype);

protected:

	/**
	Event-loop of the message-handler.
	Thread handling the client-connection.
	*/
	void Run();

	/**
	Message packet handler.
	This handler should be re-implemented to handle messages forwarding

	@param request pointer to incoming message packet.
	@param response pointer to outgoing response packet.
	@return return true if the packet was processed successfully
	*/
	bool OnMessage(MsgPacket* request, MsgPacket* response);

	/**
	Get the JSON format of a response.
	Check is there is a JSON format for a response available (must be implemented by subclasses).
	@param response pointer to response packet
	@param jsonformat JSON string containing the format of the response (out)
	@return true if a JSON format is available
	*/
	virtual bool OnResponseFormat(MsgPacket* response, std::string& jsonformat);

	/**
	Generate a custom JSON response.
	Create a JSON result string for a response
	@param response pointer to a response packet
	@param result JSON result string (out)
	@return true if a custom JSON response was generated
	*/
	virtual bool OnCustomJSONResponse(MsgPacket* response, std::string& result);

private:

	bool WaitForData();

	bool ReceiveString(std::string& str);

	bool SendString(const std::string& str, bool bTerminate = true);

	bool SendHTTPResponse(const std::string& json);

	int m_msgtype;

	bool m_closed;
};

/*
@startuml

note top of MsgHandlerJSON
 <b>JSON Connection Handler</b>
 Handles JSON messages.

 <b>Sources</b>
 libmsgexchange/include/msghandlerjson.h
 libmsgexchange/src/msghandlerjson.cpp
end note

MsgHandlerJSON --|> MsgHandler

class MsgHandlerJSON {
+void SetMessageType(int msgtype)
#void Run()
#bool OnMessage(MsgPacket* request, MsgPacket* response)
#bool OnResponseFormat(MsgPacket* response, std::string& jsonformat)
#bool OnCustomJSONResponse(MsgPacket* response, std::string& result)
-bool WaitForData()
-bool ReceiveString(std::string& str)
-bool SendString(const std::string& str, bool bTerminate = true)
-int m_msgtype
-bool m_closed
}

@enduml
*/
#endif // MSGHANDLERJSON_H
