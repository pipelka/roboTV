#include <stdio.h>
#include <iostream>

#include "msgserver.h"
#include "msghandlerjson.h"

class MyHandlerJSON : public MsgHandlerJSON {
public:

	MyHandlerJSON(int sock) : MsgHandlerJSON(sock) {
	}

	bool OnMessage(MsgPacket* request, MsgPacket* response) {
		std::cout << "JSONServer::OnMessage()" << std::endl;
		response->put_Blob(request->getPayload(), request->getPayloadLength());
		return true;
	}

	bool OnResponseFormat(MsgPacket* response, std::string& jsonformat) {
		jsonformat = "{ \"type\": 2, \"packet\": [{\"name\": \"param1\", \"type\": \"string\", \"value\": \"\" },{\"name\": \"param2\", \"type\": \"uint8\", \"value\": 0 }] }";
		std::cout << "OnResponseFormat: " << jsonformat << std::endl;
		return true;
	}
};

class JSONServer : public MsgServer {
public:

	JSONServer(int port) : MsgServer(port) {};

protected:

	MsgHandler* CreateHandler(int sock) {
		return new MyHandlerJSON(sock);
	}
};

int main(int argc, char* argv[]) {
	char buffer[20];

	JSONServer js(7777);
	js.Start();
	gets(buffer);
	js.Stop();
}
