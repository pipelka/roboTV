#include "msgserver.h"
#include "msgpacket.h"
#include "msgsession.h"
#include "msgcallbacks.h"

#include <iostream>
#include <stdio.h>

class ListenerClient : public MsgCallbacks<ListenerClient, MsgSession> {
public:

	ListenerClient() {
		RegisterNotificationHandler(1000, &ListenerClient::OnNotification1000);
	}

protected:

	void OnNotification1000(MsgPacket* p) {
		std::cout << "-------------------------------------------" << std::endl;
		std::cout << "NOTIFICATION" << std::endl;
		p->print();
	}
};

int main(int argc, char* argv[]) {
	char buffer[20];

	ListenerClient listener;

	if(!listener.Open("localhost", 12001)) {
		std::cerr << "unable to open connection !" << std::endl;
		return 1;
	}

	for(;;) {
		for(int i = 0; i < 5; i++) {
			std::cout << "-------------------------------------------" << std::endl;
			std::cout << "Sending Message " << (i + 1) << std::endl;

			MsgPacket p(123, 2);
			p.reserve(1024, true, 0xAA);
			p.put_String("testmessage");
			p.put_U8(128);
			p.put_S32(-453786);
			p.put_U32(434345);
			p.put_U64(43748364343ULL);
			p.put_S64(-454746546464343LL);

			p.setClientID(1000 + i);

			p.compress(9);

			std::cout << "Waiting for response ..." << std::endl;
			MsgPacket* resp = listener.TransmitMessage(&p);

			if(resp == NULL) {
				std::cout << "FAILED !!!" << std::endl;
				continue;
			}

			std::cout << "Checking response ..." << std::endl;
			resp->consume(1024);

			if((std::string)resp->get_String() != "testmessage" || (int)resp->get_U8() != 128 || resp->get_S32() != -453786 || resp->get_U32() != 434345 || resp->get_U64() != 43748364343ULL || resp->get_S64() != -454746546464343LL) {
				std::cout << "FAILED !!!" << std::endl;
			}
			else {
				std::cout << "CORRECT" << std::endl;
			}

			delete resp;

			MsgPacket p1(200, 0);
			p1.disablePayloadCheckSum();

			std::cout << "Waiting for response ..." << std::endl;
			resp = listener.TransmitMessage(&p1);

			if(resp == NULL) {
				std::cout << "FAILED !!!" << std::endl;
				continue;
			}

			std::cout << "Checking response ..." << std::endl;

			if((std::string)resp->get_String() != "ok200") {
				std::cout << "FAILED !!!" << std::endl;
			}
			else {
				std::cout << "CORRECT" << std::endl;
			}

			delete resp;
		}

		char* l = gets(buffer);
	}

	listener.Close();
	return 0;
}
