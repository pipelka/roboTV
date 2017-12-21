/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include <unistd.h>
#include <sys/time.h>
#include <iostream>
#include "errno.h"

#include "msgsession.h"
#include "msgpacket.h"

MsgSession::MsgResponseCondition::MsgResponseCondition() : packet(nullptr) {
}

MsgSession::MsgResponseCondition::MsgResponseCondition(MsgPacket* destinationPacket) : packet(destinationPacket) {
}

void MsgSession::MsgResponseCondition::Signal(MsgPacket* p) {
	if(packet != nullptr) {
		packet->copy(p);
	}
	else {
		packet = p;
	}

	MsgCondition::Signal();
}


MsgSession::MsgSession() {
}

MsgSession::~MsgSession() {
}

bool MsgSession::Open(const char* hostname, int port) {
	std::lock_guard<std::mutex> lock(m_mutexTransmit);

	if(!MsgConnection::Open(hostname, port)) {
		return false;
	}

	if(!MsgThread::Start()) {
		return false;
	}

	return true;
}

bool MsgSession::Close() {
	std::lock_guard<std::mutex> lock(m_mutexTransmit);
	Terminate();
	return true;
}

bool MsgSession::Terminate() {
	Abort();
	MsgThread::Stop();
	return MsgConnection::Close();
}

void MsgSession::Run() {
	MsgPacket* p = new MsgPacket();

	while(IsRunning() || !emptyQueue()) {

		// read packet from socket
		bool rc = ReadResponse(p);

		// connection lost ?
		if(GetConnectionLost()) {
			break;
		}

		// no message
		if(!rc) {
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
		}
	}

	delete p;
	m_queue.clear();
}

void MsgSession::OnNotification(MsgPacket* notification) {
}

bool MsgSession::TransmitMessage(MsgPacket* request, MsgPacket* response) {
	std::lock_guard<std::mutex> lock(m_mutexTransmit);

	if((!IsOpen() || IsAborting()) && !GetConnectionLost()) {
		return false;
	}

	// new condition
	MsgResponseCondition* q = new MsgResponseCondition(response);
	uint64_t uid = ((uint64_t)request->getUID() << 32) | request->getType();

	// add to queue
	{
		std::lock_guard<std::mutex> lock(m_mutex);
		m_queue[uid] = q;
	}

	// send message
	if(!SendRequest(request)) {
		std::lock_guard<std::mutex> lock(m_mutex);
		m_queue.erase(uid);
		delete q;
		return false;
	}

	// wait for response
	bool rc = q->Wait(m_timeout);

	{
		std::lock_guard<std::mutex> lock(m_mutex);
		m_queue.erase(uid);
		delete q;
	}

	return rc;
}

MsgPacket* MsgSession::TransmitMessage(MsgPacket* message) {
	MsgPacket* response = new MsgPacket();

	if(!TransmitMessage(message, response)) {
		delete response;
		return nullptr;
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
