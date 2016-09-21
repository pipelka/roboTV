/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>

#include "os-config.h"
#include "msgserver.h"
#include "msghandlerbase.h"

MsgServer::MsgServer(int port) : m_port(port), m_server(-1), m_persistance(PERSIST_CONNECTION_NONE) {
}

MsgServer::~MsgServer() {
	closelog();
}

bool MsgServer::Start() {
	std::lock_guard<std::mutex> lock(m_mutex);

	syslog(LOG_INFO, "starting");

	m_server = CreateServerSocket();

	if(m_server == -1) {
		syslog(LOG_ERR, "unable to create server socket");
		return false;
	}

	m_handlers.Start();
	return MsgThread::Start();
}

bool MsgServer::Stop() {
	std::lock_guard<std::mutex> lock(m_mutex);

	syslog(LOG_INFO, "shutting down");
	shutdown(m_server, SHUT_RDWR);

	bool rc = MsgThread::Stop();

	syslog(LOG_INFO, "removing handlers");
	m_handlers.StopAll();
	m_handlers.Stop();

	close(m_server);

	syslog(LOG_INFO, "server stopped");
	return rc;
}

int MsgServer::CreateServerSocket() {
	int sock = socket(AF_INET, SOCK_STREAM, 0);

	if(sock == -1) {
		syslog(LOG_DEBUG, "Unable to create server socket");
		return sock;
	}

	int one = 1;
	setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (sockval_t*)&one, sizeof(int));

	struct sockaddr_in s;
	memset(&s, 0, sizeof(s));
	s.sin_family = AF_INET;
	s.sin_port = htons(m_port);

	if(bind(sock, (struct sockaddr*)&s, sizeof(s)) < 0) {
		close(sock);
		sock = -1;
		return sock;
	}

	if(listen(sock, 10) == -1) {
		syslog(LOG_DEBUG, "Unable to listen()");
		close(sock);
		sock = -1;
	}

	return sock;
}

void MsgServer::Run() {

	while(IsRunning()) {
		if(!pollfd(m_server, 5000, true)) {
			continue;
		}

		if(!IsRunning()) {
			break;
		}

		syslog(LOG_INFO, "waiting for connection ...");

		struct sockaddr addr;
		socklen_t addrlen = sizeof(addr);

		int fd = accept(m_server, 0, 0);

		if(fd == -1) {
			continue;
		}

		if(!IsRunning()) {
			break;
		}

		std::lock_guard<std::mutex> lock(m_mutex);

		syslog(LOG_INFO, "incoming connection");

		// lookup persistent connection
		uint64_t clientid = 0;

		if(getpeername(fd, &addr, &addrlen) == 0) {
			clientid = (uint64_t)(((struct sockaddr_in*)&addr)->sin_addr.s_addr);
		}
		else {
			syslog(LOG_INFO, "getpeername: %s", strerror(errno));
		}

		MsgHandlerBase* h = FindHandlerByID(clientid);

		// create new handler
		if(h == NULL) {
			h = CreateHandler(fd);

			if(h == NULL) {
				syslog(LOG_ERR, "failed to create message handler !");
			}
			else {
				syslog(LOG_INFO, "created handler %p", h);

				h->SetPersistance(m_persistance);

				syslog(LOG_INFO, "Started handler");

				if(h->GetPersistance() != MsgServer::PERSIST_CONNECTION_NONE) {
					syslog(LOG_INFO, "persistent connection %08lX on fd %i", clientid, fd);
					h->SetClientID(clientid);
				}

				m_handlers.Run(h);
				m_handlers.Awake();
			}

			continue;
		}

		// multiple connections from same ip ?
		if(h->IsDisconnected() == false && h->GetPersistance() != MsgServer::PERSIST_CONNECTION_NONE) {
			syslog(LOG_INFO, "Cloning handler %08lx", clientid);
			MsgHandlerBase* n = h->Clone(fd);
			n->SetClientID(0);
			n->SetPersistance(MsgServer::PERSIST_CONNECTION_NONE);

			m_handlers.Run(n);

			syslog(LOG_INFO, "Cloned handler %08lx (%p) on fd %i.", clientid, n, fd);
		}

		// reconnected client ?
		else  {
			// check if the handler is sleeping, if not
			// it's possible we enter a race condition
			// -> calling Awake() before Sleep()
			while(!h->IsSleeping()) {
				// wait for the handler to enter sleep mode
				usleep(1 * 1000);
			}

			syslog(LOG_INFO, "reusing connection %08lX on fd %i", clientid, fd);
			h->SetSocket(fd);
			h->Awake();
			h->OnClientReconnect();
		}
	}
}

bool MsgServer::SetPort(int port) {
	bool running = false;

	{
		if(port == m_port) {
			return true;
		}

		if(!m_handlers.empty()) {
			return false;
		}

		running = IsRunning();
	}

	if(running) {
		if(!Stop()) {
			return false;
		}
	}

	m_port = port;

	if(running) {
		return Start();
	}

	return true;
}

MsgHandlerBase* MsgServer::FindHandlerByID(uint64_t id) {
	return (MsgHandlerBase*)m_handlers.FindByID(id);
}

void MsgServer::SetPersistance(enum MsgServer::PersistanceMethod method) {
	m_persistance = method;
}

void MsgServer::foreach(std::function<void(MsgThread*)> fn) {
	m_handlers.foreach(fn);
}
