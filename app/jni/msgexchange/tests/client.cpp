#include "msgsession.h"
#include "msgpacket.h"
#include <iostream>

int main(int argc, char* argv[]) {
	MsgSession c;

	for(int j = 0; j < 10000; j++) {
		if(!c.Open("127.0.0.1", 12001)) {
			std::cout << "unable to open connection !!!" << std::endl;
			return 1;
		}

		for(int i = 0; i < 5; i++) {
			std::cout << "-------------------------------------------" << std::endl;
			std::cout << "Sending Message " << (i + 1) << std::endl;

			MsgPacket p(123, 0);
			p.reserve(1024, true, 0xAA);
			p.put_String("testmessage");
			p.put_U8(128);
			p.put_S32(-453786);
			p.put_U32(434345);
			p.put_U64(43748364343ULL);
			p.put_S64(-454746546464343LL);

			p.setClientID(1000 + i);

			p.compress(9);

			MsgPacket* resp = c.TransmitMessage(&p);

			if(resp != NULL) {
				resp->print();
				resp->consume(1024);
				std::cout << resp->get_String() << std::endl;
				std::cout << (int)resp->get_U8() << std::endl;
				std::cout << resp->get_S32() << std::endl;
				std::cout << resp->get_U32() << std::endl;
				std::cout << resp->get_U64() << std::endl;
				std::cout << resp->get_S64() << std::endl;
			}

			delete resp;
		}

		c.Close();
	}
}
