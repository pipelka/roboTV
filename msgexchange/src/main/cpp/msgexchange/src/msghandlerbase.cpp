/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include <unistd.h>
#include <fcntl.h>

#include "os-config.h"
#include "msghandlerbase.h"

MsgHandlerBase::MsgHandlerBase(int sock) : m_sock(sock), m_timeout(3000), m_persistance(MsgServer::PERSIST_CONNECTION_NONE), m_disconnected(false) {
	if(!setsock_nonblock(m_sock)) {
		syslog(LOG_ERR, "Error setting control socket to nonblocking mode");
	}

	int val = 1;
	setsockopt(m_sock, SOL_SOCKET, SO_KEEPALIVE, (sockval_t*)&val, sizeof(val));

	val = 1;
	setsockopt(m_sock, IPPROTO_TCP, TCP_NODELAY, (sockval_t*)&val, sizeof(val));
}

MsgHandlerBase::~MsgHandlerBase() {
	syslog(LOG_INFO, "message handler removed");
	shutdown(m_sock, SHUT_RDWR);
	closesocket(m_sock);
}

bool MsgHandlerBase::Start() {
	std::lock_guard<std::mutex> lock(m_mutex);
	return MsgThread::Start();
}

bool MsgHandlerBase::Stop() {
	std::lock_guard<std::mutex> lock(m_mutex);

	syslog(LOG_INFO, "removing message handler %p", this);
	shutdown(m_sock, SHUT_RDWR);

	bool rc = MsgThread::Stop();
	closesocket(m_sock);

	return rc;
}

void MsgHandlerBase::SetTimeout(int timeout_ms) {
	m_timeout = timeout_ms;
}

enum MsgServer::PersistanceMethod MsgHandlerBase::GetPersistance() const {
	return m_persistance;
}

void MsgHandlerBase::SetPersistance(enum MsgServer::PersistanceMethod method) {
	m_persistance = method;

}

void MsgHandlerBase::SetClientID(uint64_t id) {
	SetID(id);
}

uint64_t MsgHandlerBase::GetClientID() const {
	return GetID();
}

void MsgHandlerBase::SetSocket(int sock) {
	std::lock_guard<std::mutex> lock(m_mutex);

	if(m_sock != sock) {
		syslog(LOG_INFO, "changing socket descriptor");
		shutdown(m_sock, SHUT_RDWR);
		closesocket(m_sock);
	}

	m_sock = sock;
	m_disconnected = false;
}

void MsgHandlerBase::OnClientReconnect() {
	syslog(LOG_INFO, "client reconnected.");
}

bool MsgHandlerBase::IsDisconnected() const {
	return  m_disconnected;
}

void MsgHandlerBase::Lock() {
	m_lock.lock();
}

bool MsgHandlerBase::TryLock() {
	return m_lock.try_lock();
}

void MsgHandlerBase::Unlock() {
	m_lock.unlock();
}
