#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <unistd.h>

#ifdef HAVE_GTEST_GTEST_H

#include <gtest/gtest.h>
#include <fstream>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>

#include "msgserver.h"
#include "msgsession.h"
#include "msgpacket.h"
#include "msgjson.h"
#include "msghandlerjson.h"
#include "msgcallbacks.h"
#include "msgthreadqueue.h"

class MsgPacketTest : public  ::testing::Test {
public:

	MsgPacketTest() {
	}

};

TEST_F(MsgPacketTest, ReadWriteFile) {
	MsgPacket p(123, 0);
	p.reserve(1024, true, 0xAA);
	p.put_String("testmessage");
	p.put_U8(128);
	p.put_S32(-453786);
	p.put_U32(434345);
	p.put_U64(43748364343ULL);
	p.put_S64(-454746546464343LL);

	p.setClientID(1000);

	p.compress(9);

	std::fstream file;

	int fd = open("packet.bin", O_WRONLY | O_CREAT | O_TRUNC, 0666);

	EXPECT_TRUE(fd != -1);
	EXPECT_TRUE(p.write(fd, 1000));

	close(fd);

	fd = open("packet.bin", O_RDONLY, 0666);
	EXPECT_TRUE(fd != -1);

	MsgPacket* msg = MsgPacket::read(fd, 1000);
	EXPECT_TRUE(msg != NULL);
	EXPECT_TRUE(msg->uncompress());
	EXPECT_TRUE(msg->consume(1024) != NULL);
	EXPECT_TRUE(strcmp(msg->get_String(), "testmessage") == 0);

	delete msg;
	close(fd);
}

TEST_F(MsgPacketTest, ReadWriteStream) {
	MsgPacket p(123, 0);
	p.reserve(1024, true, 0xAA);
	p.put_String("testmessage");
	p.put_U8(128);
	p.put_S32(-453786);
	p.put_U32(434345);
	p.put_U64(43748364343ULL);
	p.put_S64(-454746546464343LL);

	p.setClientID(1001);
	p.compress(9);

	// stream write

	std::fstream file;
	file.open("streampacket.bin", std::ios_base::out | std::ios_base::in | std::ios_base::trunc | std::ios_base::binary);

	EXPECT_TRUE(file.is_open());

	file << p;
	EXPECT_TRUE(file.good());

	file.close();

	// stream read

	file.open("streampacket.bin", std::ios_base::in | std::ios_base::binary);

	MsgPacket msg;

	file >> msg;
	EXPECT_TRUE(file.good());

	EXPECT_TRUE(msg.uncompress());
	EXPECT_TRUE(msg.consume(1024) != NULL);
	EXPECT_TRUE(strcmp(msg.get_String(), "testmessage") == 0);

	file.close();
}


class ClientServerTest : public MsgServer, public  ::testing::Test {
public:

	ClientServerTest() : MsgServer(12001) {
	}

	void SetUp() {
		SetPersistance(MsgServer::PERSIST_CONNECTION_NONE);
	}

	void TearDown() {
	}

protected:

	class ReflectorHandler : public MsgCallbacks<ReflectorHandler> {
	public:

		ReflectorHandler(int fd) : MsgCallbacks<ReflectorHandler>::MsgCallbacks(fd) {
			m_value = 1111;

			RegisterMessageHandler(200, &ReflectorHandler::HandleMessage200);
			RegisterMessageHandler(201, &ReflectorHandler::SetValue);
			RegisterMessageHandler(202, &ReflectorHandler::GetValue);
			RegisterMessageHandler(203, &ReflectorHandler::QueuePower);

			SetQueueSize(10);
		};

		MsgHandlerBase* Clone(int fd) {
			return new ReflectorHandler(fd);
		}

	protected:

		bool HandleMessage200(MsgPacket* request, MsgPacket* response) {
			response->put_String("ok200");
			return true;
		}

		bool SetValue(MsgPacket* request, MsgPacket* response) {
			m_value = request->get_U32();
			response->put_U32(1);
			return true;
		}

		bool GetValue(MsgPacket* request, MsgPacket* response) {
			response->put_U32(m_value);
			return true;
		}

		bool QueuePower(MsgPacket* request, MsgPacket* response) {
			for(int i = 0; i < 100; i++) {
				MsgPacket* n = new MsgPacket(1100 + i, 3);
				n->disablePayloadCheckSum();
				n->put_String("queue");
				n->put_U32(i);
				QueueNotification(n);
			}

			return true;
		}

		bool OnMessage(MsgPacket* request, MsgPacket* response) {
			if(MsgCallbacks<ReflectorHandler>::OnMessage(request, response)) {
				return true;
			}

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

	private:

		uint32_t m_value;
	};

	MsgHandler* CreateHandler(int sock) {
		return new ReflectorHandler(sock);
	}
};

TEST_F(ClientServerTest, SessionTest) {
	MsgSession c;

	EXPECT_FALSE(c.Open("127.0.0.1", 12001));

	Start();

	MsgPacket resp;

	for(int j = 0; j < 100; j++) {
		EXPECT_TRUE(c.Open("127.0.0.1", 12001));

		for(int i = 0; i < 5; i++) {
			MsgPacket p(123, 0);
			p.reserve(1024, true, 0xAA);
			p.put_String("testmessage");
			p.put_String("");
			p.put_U8(128);
			p.put_S32(-453786);
			p.put_U32(434345);
			p.put_U64(43748364343ULL);
			p.put_S64(-454746546464343LL);

			p.setClientID(1000 + i);

			p.compress(9);

			EXPECT_TRUE(c.TransmitMessage(&p, &resp));

			resp.consume(1024);
			EXPECT_TRUE(strcmp(resp.get_String(), "testmessage") == 0);
			EXPECT_TRUE(strcmp(resp.get_String(), "") == 0);
			EXPECT_EQ(128, (int)resp.get_U8());
			EXPECT_EQ(-453786, resp.get_S32());
			EXPECT_EQ(434345, resp.get_U32());
			EXPECT_EQ(43748364343ULL, resp.get_U64());
			EXPECT_EQ(-454746546464343LL, resp.get_S64());
			EXPECT_TRUE(strcmp(resp.get_String(), "") == 0);
			EXPECT_TRUE(strcmp(resp.get_String(), "") == 0);
			EXPECT_TRUE(strcmp(resp.get_String(), "") == 0);
			EXPECT_EQ(0, resp.get_U8());
			EXPECT_EQ(0, resp.get_U16());
			EXPECT_EQ(0, resp.get_S16());
			EXPECT_EQ(0, resp.get_U32());
			EXPECT_EQ(0, resp.get_S32());
			EXPECT_EQ(0, resp.get_U64());
			EXPECT_EQ(0, resp.get_S64());
		}

		c.Close();
	}

	Stop();
}

TEST_F(ClientServerTest, GetHostnameTest) {
	MsgSession c;

	EXPECT_TRUE(c.GetHostname().empty());

	Start();
	EXPECT_TRUE(c.Open("127.0.0.1", 12001));
	EXPECT_EQ("127.0.0.1", c.GetHostname());

	c.Close();
	Stop();
}

TEST_F(ClientServerTest, GarbageMessage) {
	MsgSession c;

	Start();
	c.SetTimeout(100);

	EXPECT_TRUE(c.Open("127.0.0.1", 12001));

	for(int i = 0; i < 10; i++) {
		MsgPacket msg;
		uint8_t* p = msg.getPacket();

		for(int j = 0; j < 32; j++) {
			*p++ = rand() % 256;
		}

		MsgPacket* response = c.TransmitMessage(&msg);
		EXPECT_EQ(NULL, response);

		delete response;
	}

	c.Close();
	Stop();
}

TEST_F(ClientServerTest, GarbageMessageWithSync) {
	MsgSession c;

	Start();
	c.SetTimeout(100);

	EXPECT_TRUE(c.Open("127.0.0.1", 12001));

	for(int i = 0; i < 10; i++) {
		MsgPacket msg;
		uint8_t* p = msg.getPacket();

		for(int j = 0; j < 32; j++) {
			*p++ = rand() % 256;
		}

		p = msg.getPacket();
		uint32_t value = 0xAAAAAA00;
		memcpy((void*)&p, (void*)&value, sizeof(uint32_t));
		msg.freeze();

		MsgPacket* response = c.TransmitMessage(&msg);
		EXPECT_EQ(NULL, response);

		delete response;
	}

	c.Close();
	Stop();
}

TEST_F(ClientServerTest, ConnectMultipleParallel) {
	MsgThreadQueue queue;

	Start();

	auto connectClient = [this]() {
		MsgSession c;
		c.SetTimeout(30000);

		EXPECT_TRUE(c.Open("127.0.0.1", 12001));
		std::this_thread::sleep_for(std::chrono::milliseconds(500));
		EXPECT_TRUE(c.Close());
	};

	for(int i = 0; i < 100; i++) {
		queue.RunFunction(connectClient);
	}

	queue.StopAll();
	queue.Stop();

	Stop();
}

class ListenerClient : public MsgCallbacks<ListenerClient, MsgSession> {
public:

	ListenerClient() {
		RegisterNotificationHandler(1000, &ListenerClient::OnNotification1000);
	}

protected:

	void OnNotification1000(MsgPacket* p) {
		EXPECT_TRUE(p != NULL);
	}
};

TEST_F(ClientServerTest, ListenerTest) {
	char buffer[20];

	ListenerClient listener;
	listener.SetTimeout(1000);

	Start();

	for(int j = 0; j < 5; j++) {
		EXPECT_TRUE(listener.Open("localhost", 12001));

		for(int i = 0; i < 1000; i++) {
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

			MsgPacket* resp = listener.TransmitMessage(&p);

			ASSERT_TRUE(resp != NULL);

			resp->consume(1024);

			EXPECT_TRUE((std::string)resp->get_String() == "testmessage");
			EXPECT_EQ((int)resp->get_U8(), 128);
			EXPECT_EQ(resp->get_S32(), -453786);
			EXPECT_EQ(resp->get_U32(), 434345);
			EXPECT_EQ(resp->get_U64(), 43748364343ULL);
			EXPECT_EQ(resp->get_S64(), -454746546464343LL);

			delete resp;

			MsgPacket p1(200, 0);
			p1.disablePayloadCheckSum();

			resp = listener.TransmitMessage(&p1);

			ASSERT_TRUE(resp != NULL);
			EXPECT_TRUE((std::string)resp->get_String() == "ok200");

			delete resp;
		}

		listener.Close();
	}

	Stop();
}

TEST_F(ClientServerTest, QueueTest) {
	ListenerClient listener;
	listener.SetTimeout(1000);

	Start();
	EXPECT_TRUE(listener.Open("localhost", 12001));

	MsgPacket p(203, 2);
	p.put_String("testmessage");

	MsgPacket* resp = listener.TransmitMessage(&p);
	ASSERT_TRUE(resp != NULL);
	delete resp;

	listener.Close();
	Stop();
}

#ifdef ENABLE_JSON

class JSONTest : public MsgServer, public ::testing::Test {
public:

	JSONTest() : MsgServer(7777) {
	}

protected:

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
			jsonformat = "{ \"string_param\": null, \"uint8_param1\": null }";
			std::cout << "OnResponseFormat: " << jsonformat << std::endl;
			return true;
		}
	};

	MsgHandler* CreateHandler(int sock) {
		return new ClientServerTestMyHandlerJSON(sock);
	}
};

TEST_F(JSONTest, PacketConversion) {
	std::string json =
	    "{\n"
	    "\"msgid\": 23,\n"
	    "\"type\": 2,\n"
	    "\"packet\":\n"
	    "  [\n"
	    "  { \"name\": \"param1\", \"type\": \"string\", \"value\": \"string1\" },\n"
	    "  { \"name\": \"param2\", \"type\": \"uint32\", \"value\": 3433 },\n"
	    "  { \"name\": \"param3\", \"type\": \"uint8\", \"value\": 255 },\n"
	    "  { \"name\": \"param4\", \"type\": \"string\", \"value\": \"string1\" },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 111 },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 2 }\n"
	    "  ]\n"
	    "}";

	std::string jsonformat =
	    "{\n"
	    "\"packet\":\n"
	    "  [\n"
	    "  { \"name\": \"param1\", \"type\": \"string\", \"value\": \"\" },\n"
	    "  { \"name\": \"param2\", \"type\": \"uint32\", \"value\": 0 },\n"
	    "  { \"name\": \"param3\", \"type\": \"uint8\", \"value\": 0 },\n"
	    "  { \"name\": \"param4\", \"type\": \"string\", \"value\": \"\" },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 0 },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 0 }\n"
	    "  ]\n"
	    "}";

	MsgPacket* p = MsgPacketFromJSON(json, 11);

	EXPECT_FALSE(p == NULL);

	std::string json_reverse = MsgPacketToJSON(p, jsonformat);

	EXPECT_FALSE(json_reverse.empty());

	EXPECT_TRUE(json == json_reverse);

	delete p;
}
#endif // ENABLE_JSON


class PersistTest : public ClientServerTest {
public:

	void SetUp() {
		SetPersistance(MsgServer::PERSIST_CONNECTION_IP);
		EXPECT_TRUE(Start());
	}

	void TearDown() {
		EXPECT_TRUE(Stop());
	}

};

TEST_F(PersistTest, ConnectionRestore) {
	MsgSession c;

	for(int i = 0; i < 100; i++) {
		uint32_t value = rand();

		// open connection
		ASSERT_TRUE(c.Open("127.0.0.1", 12001));

		// set value
		{
			MsgPacket p(201, 2);
			p.put_U32(value);

			MsgPacket* resp = c.TransmitMessage(&p);
			ASSERT_TRUE(resp != NULL);
			ASSERT_EQ(1, resp->get_U32());
			delete resp;
		}

		// disconnect & connect again
		c.Terminate();

		ASSERT_TRUE(c.Open("127.0.0.1", 12001));

		// get value
		{
			MsgPacket p(202, 2);

			MsgPacket* resp = c.TransmitMessage(&p);
			ASSERT_TRUE(resp != NULL);
			ASSERT_FALSE(resp->eop());
			ASSERT_EQ(value, resp->get_U32());
			delete resp;
		}

		c.Close();
	}
}

TEST_F(PersistTest, ConnectionLost) {
	MsgSession c;
	uint32_t value = 1111;

	for(int j = 0; j < 5; j++) {

		// open connection
		EXPECT_TRUE(c.Open("127.0.0.1", 12001));

		for(int i = 0; i < 5; i++) {
			// set value (must succeed)
			{
				MsgPacket p(201, 2);
				p.put_U32(value);

				MsgPacket* resp = c.TransmitMessage(&p);
				ASSERT_TRUE(resp != NULL);
				EXPECT_EQ(1, resp->get_U32());
				delete resp;
			}

			EXPECT_TRUE(Stop());

			// set value (must fail)
			{
				MsgPacket p(201, 2);
				p.put_U32(value);

				MsgPacket* resp = c.TransmitMessage(&p);
				ASSERT_TRUE(resp == NULL);
				delete resp;
			}

			EXPECT_TRUE(Start());

			// set value (must succeed)
			value = 2222;
			{
				MsgPacket p(201, 2);
				p.put_U32(value);

				MsgPacket* resp = c.TransmitMessage(&p);
				ASSERT_TRUE(resp != NULL);
				EXPECT_EQ(1, resp->get_U32());
				delete resp;
			}

			// get value (must succeed)
			{
				MsgPacket p(202, 2);

				MsgPacket* resp = c.TransmitMessage(&p);
				ASSERT_TRUE(resp != NULL);
				EXPECT_FALSE(resp->eop());
				EXPECT_EQ(value, resp->get_U32());
				delete resp;
			}
		}

		c.Close();
	}
}

TEST_F(PersistTest, ConnectMultiple) {
	char buffer[20];

	ListenerClient listener1;
	listener1.SetTimeout(10000);

	ListenerClient listener2;
	listener2.SetTimeout(10000);

	for(int j = 0; j < 5; j++) {
		EXPECT_TRUE(listener1.Open("localhost", 12001));
		EXPECT_TRUE(listener2.Open("localhost", 12001));

		for(int i = 0; i < 10; i++) {
			MsgPacket p1(123, 2);
			p1.reserve(1024, true, 0xAA);
			p1.put_String("testmessage");
			p1.put_U8(128 + i);
			p1.put_S32(-453786);
			p1.put_U32(434345);
			p1.put_U64(43748364343ULL);
			p1.put_S64(-454746546464343LL);

			p1.compress(9);

			MsgPacket* resp1 = listener1.TransmitMessage(&p1);

			MsgPacket p2(123, 2);
			p2.reserve(1024, true, 0xAA);
			p2.put_String("testmessage");
			p2.put_U8(130 + i);
			p2.put_S32(-453786);
			p2.put_U32(434345);
			p2.put_U64(43748364343ULL);
			p2.put_S64(-454746546464343LL);

			p2.compress(9);

			MsgPacket* resp2 = listener2.TransmitMessage(&p2);

			// check resp1
			ASSERT_TRUE(resp1 != NULL);
			resp1->consume(1024);

			EXPECT_TRUE((std::string)resp1->get_String() == "testmessage");
			EXPECT_EQ((int)resp1->get_U8(), 128 + i);
			EXPECT_EQ(resp1->get_S32(), -453786);
			EXPECT_EQ(resp1->get_U32(), 434345);
			EXPECT_EQ(resp1->get_U64(), 43748364343ULL);
			EXPECT_EQ(resp1->get_S64(), -454746546464343LL);

			// check resp2
			ASSERT_TRUE(resp2 != NULL);
			resp2->consume(1024);

			EXPECT_TRUE((std::string)resp2->get_String() == "testmessage");
			EXPECT_EQ((int)resp2->get_U8(), 130 + i);
			EXPECT_EQ(resp2->get_S32(), -453786);
			EXPECT_EQ(resp2->get_U32(), 434345);
			EXPECT_EQ(resp2->get_U64(), 43748364343ULL);
			EXPECT_EQ(resp2->get_S64(), -454746546464343LL);

			delete resp1;
			delete resp2;

			{
				MsgPacket p1(200, 0);
				p1.disablePayloadCheckSum();

				resp1 = listener1.TransmitMessage(&p1);

				MsgPacket p2(200, 0);
				p2.disablePayloadCheckSum();

				resp2 = listener2.TransmitMessage(&p2);

				ASSERT_TRUE(resp1 != NULL);
				ASSERT_TRUE(resp2 != NULL);

				EXPECT_TRUE((std::string)resp1->get_String() == "ok200");
				EXPECT_TRUE((std::string)resp2->get_String() == "ok200");
			}

			delete resp1;
			delete resp2;
		}

		listener1.Close();
		listener2.Close();
	}
}

class ThreadQueueTest : public MsgThreadQueue, public ::testing::Test {
protected:

	class TestThread : public MsgThread {
	public:
		TestThread(MsgThreadQueue* q) : MsgThread(q) {
		}

	protected:
		void Run() {
			while(IsRunning()) {
				std::this_thread::sleep_for(std::chrono::milliseconds(100));
			}
		}
	};

	class TestThread2 : public MsgThread {
	public:
		TestThread2(MsgThreadQueue* q) : MsgThread(q) {
		}

	protected:
		void Run() {
			std::this_thread::sleep_for(std::chrono::milliseconds(500));
		}
	};
};


TEST_F(ThreadQueueTest, ThreadsNotStarted) {
	for(int i = 0; i < 5; i++) {
		new TestThread(this);
	}

	StopAll();
}

TEST_F(ThreadQueueTest, ThreadsStarted) {
	for(int i = 0; i < 5; i++) {
		MsgThread* t = new TestThread(this);
		t->Start();
	}

	StopAll();
}

TEST_F(ThreadQueueTest, ThreadsStartedandStopped) {
	for(int i = 0; i < 6; i++) {
		MsgThread* t = new TestThread(this);
		t->Start();

		if(i % 2 == 0) {
			t->Stop();
		}
	}

	StopAll();
}

TEST_F(ThreadQueueTest, ThreadsAutonomic) {
	for(int i = 0; i < 10; i++) {
		MsgThread* t = new TestThread2(this);
		t->Start();

		if(i % 2 == 0) {
			t->Stop();
		}

		if(i == 5) {
			StopAll();
		}
	}
}

TEST_F(ThreadQueueTest, Lambda) {
	for(int i = 0; i < 5; i++) {
		MsgThread* t = new MsgThread(this);
		t->Start([]() {
			std::this_thread::sleep_for(std::chrono::milliseconds(300));
		});
	}
}

TEST_F(ThreadQueueTest, LambdaBound) {
	auto waitfn = [](int duration_ms) {
		std::this_thread::sleep_for(std::chrono::milliseconds(duration_ms));
	};

	for(int i = 0; i < 5; i++) {
		MsgThread* t = new MsgThread(this);
		t->Start(std::bind(waitfn, i * 50));
	}
}

TEST_F(ThreadQueueTest, SleepAwake) {
	for(int i = 0; i < 5; i++) {
		MsgThread* t = new MsgThread(this);
		t->Start([t]() {
			t->Sleep();
		});
	}

	foreach([](MsgThread* t) {
	t->Awake();
	});

	StopAll();
}

TEST_F(ThreadQueueTest, RunFunction) {
	for(int i = 0; i < 5; i++) {
		RunFunction([]() {
			std::this_thread::sleep_for(std::chrono::milliseconds(300));
		});
	}
}

TEST_F(ThreadQueueTest, StopQueuedThread) {
	for(int i = 0; i < 6; i++) {
		MsgThread* t = new TestThread(this);
		t->SetID(i);
		t->Start();
	}

	std::this_thread::sleep_for(std::chrono::milliseconds(100));

	// stop 2 threads by id
	MsgThread* thread1 = FindByID(2);

	ASSERT_TRUE(thread1 != NULL);
	EXPECT_TRUE(thread1->Stop());

	MsgThread* thread2 = FindByID(5);

	ASSERT_TRUE(thread2 != NULL);
	EXPECT_TRUE(thread2->Stop());

	StopAll();

	EXPECT_EQ(NULL, FindByID(2));
	EXPECT_EQ(NULL, FindByID(5));
}

int main(int argc, char* argv[]) {

	::testing::InitGoogleTest(&argc, argv);

	return RUN_ALL_TESTS();
}

#else

int main(int argc, char* argv[]) {
	return 0;
}

#endif
