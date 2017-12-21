// WINDOWS
#ifdef _WIN32

#define syslog(s, msg, ...) std::cout << msg << std::endl;
#define closelog()

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0501
#endif
#define SHUT_RDWR SD_BOTH
#define sockerror() WSAGetLastError()

#ifndef EINPROGRESS
#define EINPROGRESS WSAEINPROGRESS
#endif

#define SEWOULDBLOCK WSAEWOULDBLOCK

#ifndef ENOTSOCK
#define ENOTSOCK WSAENOTSOCK
#endif

#ifndef ECONNRESET
#define ECONNRESET WSAECONNRESET
#endif

#ifndef ETIMEDOUT
#define ETIMEDOUT WSAETIMEDOUT
#endif

#define sockval_t char
#define sendval_t const char

#define MSG_DONTWAIT 0
#define MSG_NOSIGNAL 0

#include <iostream>
#include <winsock2.h>
#include <ws2spi.h>
#include <ws2tcpip.h>

uint32_t htobe32(uint32_t u);
uint64_t htobe64(uint64_t u);
uint16_t htobe16(uint16_t u);
uint32_t be32toh(uint32_t u);
uint64_t be64toh(uint64_t u);
uint16_t be16toh(uint16_t u);

// LINUX / OTHER
#else

#ifdef __FreeBSD__
#include <sys/types.h>
#include <sys/endian.h>
#include <netinet/in.h>
#endif

#define SEWOULDBLOCK EAGAIN

#define closesocket close
#define sockerror() errno

#define sockval_t int*
#define sendval_t const void

#include <fcntl.h>
#include <sys/socket.h>
#include <netdb.h>
#include <errno.h>
#include <netinet/tcp.h>
#include <syslog.h>
#endif

// ANDROID
#ifdef ANDROID
#include <linux/in.h>
#ifndef __BSD_VISIBLE
#define __BSD_VISIBLE
#endif
#include <sys/endian.h>

#ifndef be16toh
#define be16toh betoh16
#endif

#ifndef be32toh
#define be32toh betoh32
#endif

#ifndef be64toh
#define be64toh betoh64
#endif
#endif // ANDROID

bool pollfd(int fd, int timeout_ms, bool in);
bool setsock_nonblock(int fd, bool nonblock = true);
void setsock_keepalive(int fd);
int socketread(int fd, uint8_t* data, int datalen, int timeout_ms);
