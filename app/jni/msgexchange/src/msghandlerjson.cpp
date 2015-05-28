/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include "msghandlerjson.h"
#include "msgjson.h"

#include <stdlib.h>
#include <strings.h>
#include <string.h>
#include <malloc.h>
#include <syslog.h>
#include <unistd.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <iostream>
#include <errno.h>

/* Converts a hex character to its integer value */
static char from_hex(char ch) {
	return isdigit(ch) ? ch - '0' : tolower(ch) - 'a' + 10;
}

/* Converts an integer value to its hex character*/
static char to_hex(char code) {
	static char hex[] = "0123456789abcdef";
	return hex[code & 15];
}

/* Returns a url-encoded version of str */
/* IMPORTANT: be sure to free() the returned string after use */
static char* url_encode(char* str) {
	char* pstr = str, *buf = (char*)malloc(strlen(str) * 3 + 1), *pbuf = buf;

	while(*pstr) {
		if(isalnum(*pstr) || *pstr == '-' || *pstr == '_' || *pstr == '.' || *pstr == '~') {
			*pbuf++ = *pstr;
		}
		else if(*pstr == ' ') {
			*pbuf++ = '+';
		}
		else {
			*pbuf++ = '%', *pbuf++ = to_hex(*pstr >> 4), *pbuf++ = to_hex(*pstr & 15);
		}

		pstr++;
	}

	*pbuf = '\0';
	return buf;
}

/* Returns a url-decoded version of str */
/* IMPORTANT: be sure to free() the returned string after use */
static char* url_decode(const char* str) {
	const char* pstr = str;
	char* buf = (char*)malloc(strlen(str) + 1), *pbuf = buf;

	while(*pstr) {
		if(*pstr == '%') {
			if(pstr[1] && pstr[2]) {
				*pbuf++ = from_hex(pstr[1]) << 4 | from_hex(pstr[2]);
				pstr += 2;
			}
		}
		else if(*pstr == '+') {
			*pbuf++ = ' ';
		}
		else {
			*pbuf++ = *pstr;
		}

		pstr++;
	}

	*pbuf = '\0';
	return buf;
}

static std::string URLDecode(const std::string& url) {
	char* s = url_decode(url.c_str());
	std::string result = s;
	free(s);

	return result;
}

MsgHandlerJSON::MsgHandlerJSON(int sock) : MsgHandler(sock), m_msgtype(0), m_closed(false) {
}

void MsgHandlerJSON::SetMessageType(int msgtype) {
	m_msgtype = msgtype;
}

void MsgHandlerJSON::Run() {
	std::string msg;
	syslog(LOG_INFO, "JSON message handler started");

	m_closed = false;

	while(IsRunning()) {
		// exit if connection was closed
		if(m_closed) {
			break;
		}

		// wait for string
		if(!ReceiveString(msg)) {
			continue;
		}

		std::cout << msg << std::endl;

		// check for http request
		if(msg.substr(0, 8) == "OPTIONS " && msg.size() > 8) {
			msg = msg.substr(8);
		}
		else if(msg.substr(0, 4) != "GET ") {
			continue;
		}

		std::string::size_type p = msg.rfind("HTTP/");

		if(p == std::string::npos) {
			continue;
		}

		msg = msg.substr(0, p);
		std::cout << "URI: " << msg << std::endl;

		// extract JSON query string
		p = msg.find("?");

		std::string url;
		std::string query;

		if(p > 0) {
			url = msg.substr(0, p);
		}

		if(p < msg.size() - 1) {
			query = URLDecode(msg.substr(p + 1));
		}

		std::cout << "URL: " << url << std::endl;
		std::cout << "QUERY: " << query << std::endl;

		// get message id
		while((url[0] > '9' || url[0] < '0') && url.size() > 1) {
			url = url.substr(1);
		}

		uint32_t msgid = atoi(url.c_str());
		MsgPacket* request = MsgPacketFromJSON(query, msgid);

		if(m_msgtype != 0) {
			request->setType(m_msgtype);
		}

		std::cout << "MSGID: " << request->getMsgID() << std::endl;
		std::cout << "MSGTYPE: " << request->getType() << std::endl;

		request->print();

		MsgPacket* response = new MsgPacket(request->getMsgID(), request->getType(), request->getUID());

		std::string jsonformat;
		std::string result;

		request->rewind();

		if(OnMessage(request, response)) {
			if(OnCustomJSONResponse(response, result)) {
				SendHTTPResponse(result);
			}
			else if(OnResponseFormat(response, jsonformat)) {
				result = MsgPacketToJSON(response, jsonformat);
				SendHTTPResponse(result);
			}
		}

		delete response;
		delete request;
	}
}

bool MsgHandlerJSON::SendHTTPResponse(const std::string& json) {
	std::cout << "Sending Response:" << std::endl;
	std::cout << "Content-Length: " << json.size() << std::endl;
	std::cout << "Content: " << json << std::endl;

	SendString("HTTP/1.1 200 OK");
	char buffer[60];
	snprintf(buffer, sizeof(buffer), "Content-Length: %lu", json.size());
	SendString(buffer);
	SendString("Connection: close");
	SendString("Content-Type: application/xml");
	SendString("");
	SendString(json);

	return true;
}

bool MsgHandlerJSON::OnMessage(MsgPacket* request, MsgPacket* response) {
	return false;
}

bool MsgHandlerJSON::OnResponseFormat(MsgPacket* response, std::string& jsonformat) {
	return false;
}

bool MsgHandlerJSON::OnCustomJSONResponse(MsgPacket* response, std::string& result) {
	return false;
}

bool MsgHandlerJSON::WaitForData() {
	fd_set set;
	struct timeval to;

	FD_ZERO(&set);
	FD_SET(m_sock, &set);

	to.tv_sec = m_timeout;
	to.tv_usec = 0;

	int rc = select(m_sock + 1, &set, NULL, NULL, &to);
	m_closed = (errno == ECONNRESET || errno == EBADF);
	return (rc > 0);
}

bool MsgHandlerJSON::ReceiveString(std::string& str) {
	static char cmd[1024];
	int bytes_read = 0;
	bool done = false;

	// wait for command
	if(!WaitForData()) {
		return false;
	}

	bzero(cmd, sizeof(cmd));

	while(!done) {
		int rc = recv(m_sock, cmd + bytes_read, sizeof(cmd) - bytes_read, MSG_DONTWAIT);

		if(rc <= 0) {
			m_closed = (rc == 0);
			return false;
		}

		bytes_read += rc;

		if(cmd[bytes_read - 1] == '\x0a' || cmd[bytes_read - 1] == '\x0d') {
			done = true;
		}
	}

	if(done) {
		str = cmd;
	}

	return done;
}

bool MsgHandlerJSON::SendString(const std::string& str, bool bTerminate) {
	int blockcount = 0;
	bool done = false;
	std::string::size_type bytes_written = 0;
	std::string newstr = str;

	if(bTerminate) {
		newstr += "\n";
	}

	while(!done && blockcount < m_timeout) {
		int rc = send(m_sock, newstr.c_str() + bytes_written, newstr.size() - bytes_written, MSG_DONTWAIT);

		if(rc == -1) {
			blockcount++;
			usleep(1000 * 1000);
			continue;
		}

		bytes_written += rc;
		done = (bytes_written == newstr.size());
	}

	return done;
}
