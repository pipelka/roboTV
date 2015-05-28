/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include "msghandler.h"
#include "os-config.h"
#include <unistd.h>

MsgHandler::MsgHandler(int sock) : MsgHandlerBase(sock), m_queuesize(0) {
}

MsgHandler::~MsgHandler() {
	// remove pending notifications
	std::lock_guard<std::mutex> lock(m_mutex);

	while(!m_queue.empty()) {
		MsgPacket* p = m_queue.front();
		m_queue.pop();
		delete p;
	}
}

void MsgHandler::Run() {
	syslog(LOG_INFO, "message handler started");

	while(IsRunning()) {
		bool closed = false;

		// read packet
		Lock();
		MsgPacket* request = MsgPacket::read(m_sock, closed, 5000);

		if(closed) {
			m_disconnected = true;
		}

		Unlock();

		if(closed) {
			if(m_persistance != MsgServer::PERSIST_CONNECTION_NONE && IsRunning()) {
				Sleep();
			}
			else {
				break;
			}
		}

		if(IsDisconnected()) {
			m_disconnected = false;
		}

		if(request != NULL) {
			MsgPacket* response = new MsgPacket(request->getMsgID(), request->getType(), request->getUID());

			// process request
			if(OnMessage(request, response)) {
				if(!response->write(m_sock, m_timeout)) {
					syslog(LOG_ERR, "failed to send response !");
				}
			}

			// delete packets
			delete response;
			delete request;
		}

		// send pending notifications
		bool done = false;

		std::lock_guard<std::mutex> lock(m_mutex);

		while(!done && IsRunning() && !IsDisconnected()) {

			MsgPacket* p = NULL;
			done = m_queue.empty();

			if(!done) {
				p = m_queue.front();
			}

			if(p != NULL) {
				if(p->write(m_sock, m_timeout)) {
					m_queue.pop();
					delete p;
				}
			}

		}
	}

	syslog(LOG_INFO, "message handler ended");
}

bool MsgHandler::OnMessage(MsgPacket* request, MsgPacket* response) {
	return true;
}

bool MsgHandler::QueueNotification(MsgPacket* packet) {
	// try to send packet directly to client
	if(!IsDisconnected() && packet->write(m_sock, m_timeout)) {
		delete packet;
		return true;
	}

	std::lock_guard<std::mutex> lock(m_mutex);

	// limit queue size
	while(m_queue.size() > m_queuesize) {
		MsgPacket* p = m_queue.front();
		m_queue.pop();

		if(p != NULL) {
			delete p;
		}
	}

	// push packet into queue
	m_queue.push(packet);

	return true;
}

MsgHandlerBase* MsgHandler::Clone(int fd) {
	return new MsgHandler(fd);
}

void MsgHandler::SetQueueSize(int queuesize) {
	m_queuesize = queuesize;
}
