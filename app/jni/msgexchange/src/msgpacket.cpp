/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#ifdef HAVE_ZLIB
#include <zlib.h>
#endif

#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <iostream>
#include <unistd.h>

#include "os-config.h"
#include "msgpacket.h"

#define get_impl(T, f) \
	if((m_readposition + sizeof(T)) > m_usage) { \
		return 0; \
	} \
	T ul = f(readPacket<T>(m_readposition)); \
	m_readposition += sizeof(T); \
	return ul

#define put_impl(T, f, v) \
	if(!checkPacketSize(sizeof(T))) { \
		return false; \
	} \
	writePacket<T>(m_usage, f(v)); \
	m_usage += sizeof(T); \
	return true

std::mutex MsgPacket::uidmutex;

uint32_t MsgPacket::globalUID = 1;

uint32_t MsgPacket::crc32_tab[] = {
	0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f,
	0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988,
	0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2,
	0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,
	0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,
	0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4, 0xa2677172,
	0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b, 0x35b5a8fa, 0x42b2986c,
	0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59,
	0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423,
	0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,
	0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190, 0x01db7106,
	0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433,
	0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d,
	0x91646c97, 0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e,
	0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,
	0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65,
	0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7,
	0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0,
	0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa,
	0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
	0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81,
	0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a,
	0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84,
	0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,
	0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,
	0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a, 0x67dd4acc,
	0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e,
	0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,
	0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55,
	0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,
	0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe, 0xb2bd0b28,
	0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d,
	0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f,
	0x72076785, 0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38,
	0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
	0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777,
	0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69,
	0x616bffd3, 0x166ccf45, 0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2,
	0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc,
	0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
	0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693,
	0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94,
	0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d
};


MsgPacket::MsgPacket() : m_packet(NULL), m_size(InitialPacketSize), m_usage(HeaderLength), m_readposition(HeaderLength), m_freezed(false), m_payloadchecksum(true) {
	Init(0, 0, 0);
}

MsgPacket::MsgPacket(uint16_t msgid, uint16_t type, uint32_t uid) : m_packet(NULL), m_size(InitialPacketSize), m_usage(HeaderLength), m_readposition(HeaderLength), m_freezed(false), m_payloadchecksum(true) {
	Init(msgid, type, uid);
}

MsgPacket::~MsgPacket() {
	free(m_packet);
}

void MsgPacket::Init(uint16_t msgid, uint16_t type, uint32_t uid) {
	m_packet = (uint8_t*)malloc(m_size);

	if(m_packet == NULL) {
		return;
	}

	std::lock_guard<std::mutex> lock(uidmutex);

	if(uid <= 0) {
		uid = globalUID++;
	}
	else if(uid > globalUID) {
		globalUID = uid + 1;
	}

	memset(m_packet, 0, HeaderLength);

	writePacket<uint32_t>(SyncPos, htobe32(0xAAAAAA));		// sync
	setUID(uid);											// packet serial number
	setMsgID(msgid);										// message id
	setType(type);											// message type
}

void MsgPacket::setClientID(uint16_t oid) {
	writePacket<uint16_t>(ClientIDPos, htobe16(oid));
}

uint16_t MsgPacket::getClientID() {
	return be16toh(readPacket<uint16_t>(ClientIDPos));
}

uint32_t MsgPacket::getCheckSum() {
	return be32toh(readPacket<uint32_t>(CheckSumPos));
}

uint32_t MsgPacket::getPayloadCheckSum() {
	return be32toh(readPacket<uint32_t>(PayloadCheckSumPos));
}

void MsgPacket::disablePayloadCheckSum() {
	m_payloadchecksum = false;
}

bool MsgPacket::put_String(const char* string) {
	uint32_t len = strlen(string) + 1;

	if(!checkPacketSize(len)) {
		return false;
	}

	memcpy(m_packet + m_usage, string, len);
	m_usage += len;

	return true;
}

bool MsgPacket::put_U8(uint8_t c) {
	put_impl(uint8_t, , c);
}

bool MsgPacket::put_U16(uint16_t us) {
	put_impl(uint16_t, htobe16, us);
}

bool MsgPacket::put_S16(int16_t s) {
	put_impl(int16_t, htobe16, s);
}

bool MsgPacket::put_U32(uint32_t ul) {
	put_impl(uint32_t, htobe32, ul);
}

bool MsgPacket::put_S32(int32_t l) {
	put_impl(int32_t, htobe32, l);
}

bool MsgPacket::put_U64(uint64_t ull) {
	put_impl(uint64_t, htobe64, ull);
}

bool MsgPacket::put_S64(int64_t ll) {
	put_impl(int64_t, htobe64, ll);
}

bool MsgPacket::put_Blob(uint8_t source[], uint32_t length) {
	uint8_t* p = reserve(length);

	if(p == NULL) {
		return false;
	}

	memcpy(p, source, length);
	return true;
}

void MsgPacket::clear() {
	m_usage = HeaderLength;
	m_readposition = HeaderLength;
}

void MsgPacket::rewind() {
	m_readposition = HeaderLength;
}

uint8_t* MsgPacket::reserve(uint32_t length, bool fill, unsigned char c) {
	if(!checkPacketSize(length)) {
		return NULL;
	}

	uint8_t* p = m_packet + m_usage;
	m_usage += length;

	if(fill) {
		memset(p, c, length);
	}

	return p;
}
uint8_t* MsgPacket::consume(uint32_t length) {
	if(m_usage < m_readposition + length) {
		return NULL;
	}

	uint8_t* p = m_packet + m_readposition;
	m_readposition += length;

	return p;
}

const char* MsgPacket::get_String() {
	if(m_readposition >= m_usage) {
		return "";
	}

	const char* value = (char*)&m_packet[m_readposition];
	size_t maxlen = m_usage - m_readposition;
	size_t length = strnlen(value, maxlen);

	m_readposition += length + 1;

	if(length == 0 || length == maxlen) {
		return "";
	}

	return value;
}

uint8_t MsgPacket::get_U8() {
	get_impl(uint8_t,);
}

uint16_t MsgPacket::get_U16() {
	get_impl(uint16_t, be16toh);
}

int16_t MsgPacket::get_S16() {
	get_impl(int16_t, be16toh);
}

uint32_t MsgPacket::get_U32() {
	get_impl(uint32_t, be32toh);
}

int32_t MsgPacket::get_S32() {
	get_impl(int32_t, be32toh);
}

uint64_t MsgPacket::get_U64() {
	get_impl(uint64_t, be64toh);
}

int64_t MsgPacket::get_S64() {
	get_impl(int64_t, be64toh);
}

bool MsgPacket::get_Blob(uint8_t dest[], uint32_t length) {
	if((m_readposition + length) > m_usage) {
		return false;
	}

	memcpy(dest, m_packet + m_readposition, length);
	m_readposition += length;

	return true;
}

uint8_t* MsgPacket::getPacket() {
	return m_packet;
}

uint32_t MsgPacket::getPacketLength() {
	return m_usage;
}

uint8_t* MsgPacket::getPayload() {
	return m_packet + HeaderLength;
}

uint32_t MsgPacket::getPayloadLength() {
	return m_usage - HeaderLength;
}

uint32_t MsgPacket::getUID() {
	return be32toh(readPacket<uint32_t>(UIDPos));
}

void MsgPacket::setUID(uint32_t uid) {
	writePacket<uint32_t>(UIDPos, htobe32(uid));
}

uint16_t MsgPacket::getMsgID() {
	return be16toh(readPacket<uint16_t>(MsgIDPos));
}

void MsgPacket::setMsgID(uint16_t msgid) {
	writePacket<uint16_t>(MsgIDPos, htobe16(msgid));
}

uint16_t MsgPacket::getType() {
	return be16toh(readPacket<uint16_t>(TypePos));
}

void MsgPacket::setType(uint16_t type) {
	writePacket<uint16_t>(TypePos, htobe16(type));
}

uint16_t MsgPacket::getProtocolVersion() {
	return be16toh(readPacket<uint16_t>(ProtocolVersionPos));
}

void MsgPacket::setProtocolVersion(uint16_t version) {
	writePacket<uint16_t>(ProtocolVersionPos, htobe16(version));
}

bool MsgPacket::eop() {
	return (m_readposition >= m_usage);
}

void MsgPacket::freeze() {
	if(m_freezed) {
		return;
	}

	uint32_t payloadCheckSum = 0;

	if(getPayloadLength() > 0 && m_payloadchecksum) {
		payloadCheckSum = crc32(m_packet + HeaderLength, m_usage - HeaderLength);
	}

	writePacket<uint32_t>(PayloadCheckSumPos, htobe32(payloadCheckSum));
	writePacket<uint32_t>(PayloadLengthPos, htobe32(m_usage - HeaderLength));
	writePacket<uint32_t>(CheckSumPos, htobe32(crc32(m_packet, CheckSumPos)));

	m_freezed = true;
}

bool MsgPacket::checkPacketSize(uint32_t bytes) {
	if(bytes == 0) {
		return false;
	}

	if((m_usage + bytes) <= m_size) {
		return true;
	}

	if(bytes < IncrementPacketSize) {
		bytes = IncrementPacketSize;
	}

	uint8_t* buffer = (uint8_t*)realloc((void*)m_packet, m_usage + bytes);

	if(buffer == NULL) {
		buffer = (uint8_t*)malloc(m_usage + bytes);

		if(buffer == NULL) {
			return false;
		}

		memcpy(buffer, m_packet, m_usage);
		free(m_packet);
	}

	m_packet = buffer;
	m_size = m_usage + bytes;
	return true;
}

uint32_t MsgPacket::crc32(const uint8_t* buf, int size) {
	uint32_t crc = 0xFFFFFFFF;
	const uint8_t* p = buf;

	while(size--) {
		crc = crc32_tab[(crc ^ *p++) & 0xFF] ^ (crc >> 8);
	}

	return (crc ^ ~0U);
}

bool MsgPacket::write(int fd, int timeout_ms) {
	freeze();

	uint32_t written = 0;

	while(written < m_usage) {
		if(pollfd(fd, timeout_ms, false) == 0) {
			return false;
		}

		int rc = send(fd, (sendval_t*)(m_packet + written), m_usage - written, MSG_DONTWAIT | MSG_NOSIGNAL);

		if(rc == -1 && sockerror() == ENOTSOCK) {
			rc = ::write(fd, m_packet + written, m_usage - written);
		}

		if(rc == -1 || rc == 0) {
			if(sockerror() == SEWOULDBLOCK) {
				continue;
			}

			return false;
		}

		written += rc;
	}

	return true;
}

MsgPacket* MsgPacket::read(int fd, int timeout_ms) {
	bool bClosed;
	return read(fd, bClosed, timeout_ms);
}

MsgPacket* MsgPacket::read(int fd, bool& closed, int timeout_ms) {
	if(pollfd(fd, timeout_ms, true) <= 0) {
		return NULL;
	}

	MsgPacket* p = new MsgPacket(0, 0, 1);

	if(p == NULL) {
		return NULL;
	}

	uint8_t* header = p->getPacket();

	if(header == NULL) {
		delete p;
		return NULL;
	}

	// try to find sync
	int rc = 0;

	while((rc = socketread(fd, header, sizeof(uint32_t), timeout_ms)) == 0) {
		uint32_t sync = be32toh(p->readPacket<uint32_t>(0));

		if(sync == 0xAAAAAA) {
			break;
		}
	}

	// not found / timeout
	if(rc != 0) {
		closed = (rc == ECONNRESET);
		delete p;
		return NULL;
	}

	// read remaining header bytes
	uint8_t* data = header + sizeof(uint32_t);
	uint32_t datalen = HeaderLength - sizeof(uint32_t);

	if(socketread(fd, data, datalen, timeout_ms) != 0) {
		delete p;
		return NULL;
	}

	// header validation
	uint32_t checksum = p->getCheckSum();
	datalen = be32toh(p->readPacket<uint32_t>(PayloadLengthPos));
	uint32_t test = crc32(header, CheckSumPos);

	if(checksum != test) {
		std::cerr << "checksum failed !" << std::endl;
		std::cerr << "PACKET CHECKSUM  : " << std::hex << checksum << std::endl;
		std::cerr << "COMPUTED CHECKSUM: " << std::hex << test << std::endl;
		delete p;
		return NULL;
	}

	// no payload ?
	if(datalen == 0) {
		return p;
	}

	// read payload
	data = p->reserve(datalen);

	if(data == NULL) {
		delete p;
		return NULL;
	}

	if(socketread(fd, data, datalen, timeout_ms) != 0) {
		delete p;
		return NULL;
	}

	// payload checksum validation
	uint32_t plcs = p->getPayloadCheckSum();
	p->m_payloadchecksum = (plcs != 0);

	if(p->m_payloadchecksum && p->getPayloadCheckSum() != crc32(data, datalen)) {
		std::cerr << "wrong payload checksum !" << std::endl;
		delete p;
		return NULL;
	}

	return p;
}

bool MsgPacket::readstream(std::istream& in, MsgPacket& p) {
	uint8_t* header = p.getPacket();

	if(header == NULL) {
		return NULL;
	}

	// try to find sync
	bool rc = false;

	while(!rc) {
		in.read((char*)header, sizeof(uint32_t));
		rc = in.good();

		uint32_t sync = be32toh(p.readPacket<uint32_t>(0));

		if(sync == 0xAAAAAA) {
			break;
		}
	}

	// not found / timeout
	if(!rc) {
		return false;
	}

	// read remaining header bytes
	uint8_t* data = header + sizeof(uint32_t);
	uint32_t datalen = HeaderLength - sizeof(uint32_t);

	in.read((char*)data, datalen);

	if(!in.good()) {
		return false;
	}

	// header validation
	uint32_t checksum = p.getCheckSum();
	datalen = be32toh(p.readPacket<uint32_t>(PayloadLengthPos));
	uint32_t test = crc32(header, CheckSumPos);

	if(checksum != test) {
		syslog(LOG_ERR, "checksum failed !");
		std::cerr << "PACKET CHECKSUM  : " << std::hex << checksum << std::endl;
		std::cerr << "COMPUTED CHECKSUM: " << std::hex << test << std::endl;
		return false;
	}

	// no payload ?
	if(datalen == 0) {
		return false;
	}

	// read payload
	data = p.reserve(datalen);

	if(data == NULL) {
		return false;
	}

	in.read((char*)data, datalen);

	if(!in.good()) {
		return false;
	}

	// payload checksum validation
	uint32_t plcs = p.getPayloadCheckSum();
	p.m_payloadchecksum = (plcs != 0);

	if(p.m_payloadchecksum && p.getPayloadCheckSum() != crc32(data, datalen)) {
		syslog(LOG_ERR, "wrong payload checksum !");
		return false;
	}

	return true;
}

bool MsgPacket::compress(int level) {
#ifndef HAVE_ZLIB
	return false;
#else

	if(level <= 0 || level > 9 || m_freezed) {
		return false;
	}

	uint32_t uncompressedsize = getPayloadLength();

	if(uncompressedsize == 0) {
		return true;
	}

	uint8_t* compressed = (uint8_t*)malloc(uncompressedsize);
	uLongf compressedsize = uncompressedsize;

	if(compressed == NULL) {
		return NULL;
	}

	if(::compress2(compressed, &compressedsize, getPayload(), uncompressedsize, level) != Z_OK) {
		free(compressed);
		return false;
	}

	clear();
	uint8_t* data = reserve(compressedsize);

	if(data == NULL) {
		free(compressed);
		return false;
	}

	memcpy(data, compressed, compressedsize);
	free(compressed);

	m_freezed = false;
	writePacket<uint32_t>(UncompressedPayloadLengthPos, htobe32(uncompressedsize));
	freeze();

	return true;
#endif
}

bool MsgPacket::isCompressed() {
	return (be32toh(readPacket<uint32_t>(UncompressedPayloadLengthPos)) != 0);
}

bool MsgPacket::uncompress() {
#ifndef HAVE_ZLIB
	return false;
#else
	uLongf uncompressedsize = be32toh(readPacket<uint32_t>(UncompressedPayloadLengthPos));
	uint8_t* uncompressed = (uint8_t*)malloc(uncompressedsize);

	if(::uncompress(uncompressed, &uncompressedsize, getPayload(), getPayloadLength()) != Z_OK) {
		free(uncompressed);
		return false;
	}

	clear();
	uint8_t* data = reserve(uncompressedsize);

	if(data == NULL) {
		free(uncompressed);
		return false;
	}

	memcpy(data, uncompressed, uncompressedsize);
	free(uncompressed);

	writePacket<uint32_t>(UncompressedPayloadLengthPos, htobe32(0));

	m_freezed = false;
	freeze();

	return true;
#endif
}

void MsgPacket::print() {
	uint32_t checksum = getCheckSum();
	uint32_t test = crc32(m_packet, CheckSumPos);
	bool sumvalid = (checksum == test);

	std::cout << "MESSAGE PACKET  #" << getUID() << std::endl;
	std::cout << "MSGID          : " << getMsgID() << std::endl;
	std::cout << "TYPE           : " << getType() << std::endl;

	if(checksum != 0) {
		std::cout << "CHECKSUM       : 0x" << std::hex << checksum << " " << (sumvalid ? "(OK)" : "(FAILED)") << std::endl;
	}
	else {
		std::cout << "NO CHECKSUM" << std::endl;
	}

	std::cout << "Owner ID       : " << std::dec << getClientID() << std::endl;
	std::cout << "Total length   : " << std::dec << getPacketLength() << " bytes" << std::endl;
	std::cout << "Header length  : " << HeaderLength << " bytes" << std::endl;
	std::cout << "Payload length : " << getPayloadLength() << " bytes" << std::endl;
	std::cout << "-------------------------------------------" << std::endl;
}
