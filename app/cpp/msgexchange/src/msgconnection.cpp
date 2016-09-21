/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>

#include "os-config.h"
#include "msgconnection.h"
#include "msgpacket.h"

MsgConnection::MsgConnection() : m_socket(-1), m_timeout(3000), m_connectionlost(false), m_port(0), m_aborting(false) {
}

MsgConnection::~MsgConnection() {
}

bool MsgConnection::Open(const char* hostname, int port) {
	if(m_socket != -1) {
		return false;
	}

	char service[10];
	snprintf(service, sizeof(service), "%i", port);

	struct addrinfo hints;
	memset(&hints, 0, sizeof(hints));
	hints.ai_family   = AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;

	struct addrinfo* result;

	if(getaddrinfo(hostname, service, &hints, &result) != 0) {
		return false;
	}

	// loop through results
	struct addrinfo* info = result;
	int sockfd = -1;

	while(sockfd == -1 && info != NULL) {
		sockfd = socket(info->ai_family, info->ai_socktype, info->ai_protocol);

		// try to connect
		if(sockfd != -1) {
			break;
		}

		info = info->ai_next;
	}

	if(sockfd == -1) {
		freeaddrinfo(result);
		return false;
	}

	setsock_nonblock(sockfd);

	int rc = 0;

	if(connect(sockfd, info->ai_addr, info->ai_addrlen) == -1) {
		if(sockerror() == EINPROGRESS || sockerror() == SEWOULDBLOCK) {

			if(!pollfd(sockfd, m_timeout, false)) {
				freeaddrinfo(result);
				close(sockfd);
				return false;
			}

			socklen_t optlen = sizeof(int);
			getsockopt(sockfd, SOL_SOCKET, SO_ERROR, (sockval_t*)&rc, &optlen);
		}
		else {
			rc = sockerror();
		}
	}

	if(rc != 0) {
		freeaddrinfo(result);
		close(sockfd);
		return false;
	}

	setsock_nonblock(sockfd, false);

	int val = 1;
	setsockopt(sockfd, IPPROTO_TCP, TCP_NODELAY, (sockval_t*)&val, sizeof(val));

	setsock_keepalive(sockfd);
    setsockopt(sockfd, SOL_SOCKET, SO_PRIORITY, &m_priority, sizeof(m_priority));

	freeaddrinfo(result);

	m_hostname = hostname;
	m_port = port;
	m_connectionlost = false;
	m_aborting = false;
	m_socket = sockfd;

	return true;
}

void MsgConnection::Abort() {
	m_aborting = true;
	shutdown(m_socket, SHUT_RDWR);
}

bool MsgConnection::Close() {
	if(!IsOpen()) {
		return false;
	}

	closesocket(m_socket);
	m_socket = -1;

	m_aborting = false;

	// wait for the server to close the socket
	usleep(50 * 1000);
	return true;
}

bool MsgConnection::IsOpen() const {
	return (m_socket != -1);
}

bool MsgConnection::IsAborting() const {
	return m_aborting;
}

void MsgConnection::SetConnectionLost() {
	if(m_connectionlost || m_aborting) {
		return;
	}

	m_connectionlost = true;
	Abort();
	Close();

	OnDisconnect();
}

bool MsgConnection::GetConnectionLost() const {
	return m_connectionlost;
}

bool MsgConnection::Reconnect() {
	syslog(LOG_INFO, "reconnecting ...");

	if(!Open(m_hostname.c_str(), m_port)) {
		return false;
	}

	syslog(LOG_INFO, "connection restored.");
	m_connectionlost = false;
	OnReconnect();

	return true;
}

void MsgConnection::OnReconnect() {
	syslog(LOG_INFO, "=== CONNECTION RESTORED ===");
}

void MsgConnection::OnDisconnect() {
	syslog(LOG_INFO, "=== CONNECTION LOST ===");
}

void MsgConnection::SetTimeout(int timeout_ms) {
	m_timeout = timeout_ms;
}

bool MsgConnection::SendRequest(MsgPacket* request) {
	if(IsAborting()) {
		return false;
	}

	// restore connection if needed
	if(GetConnectionLost() && !Reconnect()) {
		return false;
	}

	if(!request->write(m_socket, m_timeout)) {
		SetConnectionLost();
		return false;
	}

	return true;
}

bool MsgConnection::ReadResponse(MsgPacket* p) {
	if(IsAborting()) {
		return false;
	}

	// restore connection if needed
	if(GetConnectionLost() && !Reconnect()) {
		return false;
	}

	bool bClosed = false;
	bool rc = MsgPacket::read(m_socket, bClosed, p, m_timeout);

	if(bClosed) {
		SetConnectionLost();
		return false;
	}

	return rc;

}

MsgPacket* MsgConnection::ReadResponse() {
	MsgPacket* p = new MsgPacket();

	if(!ReadResponse(p)) {
		delete p;
		return nullptr;
	}

	return p;
}

MsgPacket* MsgConnection::TransmitMessage(MsgPacket* message) {
	if(!SendRequest(message)) {
		return NULL;
	}

	return ReadResponse();
}

bool MsgConnection::TransmitMessage(MsgPacket* request, MsgPacket* response) {
	if(!SendRequest(request)) {
		return false;
	}

	return ReadResponse(response);
}

bool MsgConnection::pollfd(int fd, int timeout_ms, bool in) {
	return ::pollfd(fd, timeout_ms, in);
}

const std::string& MsgConnection::GetHostname() {
	return m_hostname;
}

void MsgConnection::SetPriority(int priority) {
    m_priority = priority;
}

int MsgConnection::GetPriority() const {
    return m_priority;
}
