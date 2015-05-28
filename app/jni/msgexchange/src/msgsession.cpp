/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include <unistd.h>
#include <sys/time.h>
#include "errno.h"

#include "msgsession.h"
#include "msgpacket.h"

MsgSession::MsgResponseCondition::MsgResponseCondition() : packet(NULL) {
}

void MsgSession::MsgResponseCondition::Signal(MsgPacket* p) {
	packet = p;
	MsgCondition::Signal();
}


MsgSession::MsgSession() {
}

MsgSession::~MsgSession() {
}

bool MsgSession::Open(const char* hostname, int port) {
	if(!MsgConnection::Open(hostname, port)) {
		return false;
	}

	if(!MsgThread::Start()) {
		return false;
	}

	return true;
}

bool MsgSession::Close() {
	Terminate();
	return true;
}

bool MsgSession::Terminate() {
	Abort();
	MsgThread::Stop();
	return MsgConnection::Close();
}

void MsgSession::Run() {

	while(IsRunning() || !emptyQueue()) {

		// read packet from socket
		MsgPacket* p = ReadResponse();

		// connection lost ?
		if(GetConnectionLost()) {
			usleep(50 * 1000);
		}

		// no message
		if(p == NULL) {
			continue;
		}

		// check if someone is waiting for this packet
		MsgResponseCondition* c(NULL);
		{
			std::lock_guard<std::mutex> lock(m_mutex);

			c = find(p);

			if(c != NULL) {
				uint64_t uid = ((uint64_t)p->getUID() << 32) | p->getType();
				m_queue.erase(uid);
			}
		}

		if(c != NULL) {
			c->Signal(p);
		}

		// no one waiting (notification message) ?
		else {
			OnNotification(p);
			delete p;
		}
	}

	m_queue.clear();
}

void MsgSession::OnNotification(MsgPacket* notification) {
}

MsgPacket* MsgSession::TransmitMessage(MsgPacket* message) {
	if((!IsOpen() || IsAborting()) && !GetConnectionLost()) {
		return NULL;
	}

	MsgResponseCondition* q(NULL);
	uint64_t uid(0);
	{
		std::lock_guard<std::mutex> lock(m_mutex);

		// put message on queue
		q = new MsgResponseCondition;
		uid = ((uint64_t)message->getUID() << 32) | message->getType();

		m_queue[uid] = q;

		// send message
		if(!SendRequest(message)) {
			m_queue.erase(uid);
			delete q;
			return NULL;
		}
	}

	// wait for response
	q->Wait(m_timeout);

	MsgPacket* response = NULL;
	{
		std::lock_guard<std::mutex> lock(m_mutex);

		// get response packet
		response = q->packet;

		// remove condition from queue if there wasn't any response
		if(q->packet == NULL) {
			m_queue.erase(uid);
		}

		delete q;
	}

	return response;
}

MsgSession::MsgResponseCondition* MsgSession::find(MsgPacket* p) {
	uint64_t uid = ((uint64_t)p->getUID() << 32) | p->getType();
	auto i = m_queue.find(uid);

	if(i != m_queue.end()) {
		return i->second;
	}

	return NULL;
}

bool MsgSession::emptyQueue() {
	std::lock_guard<std::mutex> lock(m_mutex);
	return m_queue.empty();
}
