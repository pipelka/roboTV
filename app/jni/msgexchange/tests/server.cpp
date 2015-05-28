#include "msgserver.h"
#include "msgpacket.h"
#include "msgcallbacks.h"

#include <stdio.h>
#include <string.h>
#include <iostream>

class ReflectorHandler : public MsgCallbacks<ReflectorHandler> {
public:

	ReflectorHandler(int fd) : MsgCallbacks<ReflectorHandler>::MsgCallbacks(fd) {
		RegisterMessageHandler(200, &ReflectorHandler::HandleMessage200);
	};

protected:

	bool HandleMessage200(MsgPacket* request, MsgPacket* response) {
		response->put_String("ok200");
		return true;
	}

	bool OnMessage(MsgPacket* request, MsgPacket* response) {
		if(MsgCallbacks<ReflectorHandler>::OnMessage(request, response)) {
			return true;
		}

		std::cout << ">> ID: " << request->getMsgID() << " TYPE: " << request->getType() << std::endl;

		// send notification
		if(request->getType() == 2) {
			MsgPacket* n = new MsgPacket(1000, 3);
			n->disablePayloadCheckSum();
			n->put_String("notification");
			n->put_U32(434345);
			QueueNotification(n);
		}

		// uncompress
		if(request->isCompressed()) {
			request->uncompress();
		}

		// send response
		response->put_Blob(request->getPayload(), request->getPayloadLength());
		response->setClientID(request->getClientID());

		return true;
	}
};

class ReflectorServer : public MsgServer {
public:

	ReflectorServer(int port) : MsgServer(port) {};

protected:

	MsgHandler* CreateHandler(int sock) {
		return new ReflectorHandler(sock);
	}
};

int main(int argc, char* argv[]) {
	char buffer[20];

	ReflectorServer s(12001);
	s.Start();
	gets(buffer);
	s.Stop();
}
