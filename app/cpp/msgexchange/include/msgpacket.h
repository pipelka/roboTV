/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgpacket.h
	Header file for the MsgPacket class.
	This include file defines the MsgPacket class
*/

#ifndef MSGPACKET_H
#define MSGPACKET_H

#include <stdint.h>
#include <string.h>

#include <ostream>
#include <mutex>
#include <istream>

// PACKET HEADER DEFINITION

// pos    type       description
// 0      uint32_t   packet sync (0xAAAAAA00)
// 4      uint32_t   packet serial number
// 8      uint16_t   message id
// 10     uint16_t   type
// 12     uint16_t   owner id
// 14     uint16_t   protocol version
// 16     uint32_t   payload checksum (0 if payload checksums are disabled)
// 20     uint32_t   payload length
// 24     uint32_t   uncompressed payload length (indicates compression if > 0)
// 28     uint32_t   header checksum

/**
	@short Message Packet class

	The primary interface for messages passed between communication endpoints.
	A packet consist of a header and a payload part
*/

class MsgPacket {
public:

	/**
	MsgPacket constructor.
	Creates a message with a given message id, type and optional serial number.

	@param	msgid			user defined message id
	@param	type			user defined message type (default: 0)
	@param	uid				packet uid (default: unique incremental id)
	*/
	MsgPacket(uint16_t msgid, uint16_t type = 0, uint32_t uid = 0);

	/**
	MsgPacket constructor.
	Creates an empty message (may be used with C++ stream operators).
	*/
	MsgPacket();

	/**
	Destructor.
	*/
	~MsgPacket();

        void createUid();

	/**
	Insert NULL terminated string.
	Add a NULL terminted string to the payload of the packet.

	@param	string		NULL terminated character string
	@return true on success / false on memory allocation error
	*/
	bool put_String(const char* string);

	/**
	Insert unsigned 8bit integer.
	Adds an unsigned 8bit integer number to the payload of the packet.

	@param	c		unsigned 8bit number
	@return true on success / false on memory allocation error
	*/
	bool put_U8(uint8_t c);

	/**
	Insert unsigned 16bit integer.
	Adds an unsigned 16bit integer number to the payload of the packet.

	@param	us		unsigned 16bit number
	@return true on success / false on memory allocation error
	*/
	bool put_U16(uint16_t us);

	/**
	Insert signed 16bit integer.
	Adds an signed 16bit integer number to the payload of the packet.

	@param	s		signed 16bit number
	@return true on success / false on memory allocation error
	*/
	bool put_S16(int16_t s);

	/**
	Insert unsigned 32bit integer.
	Adds an unsigned 32bit integer number to the payload of the packet.

	@param	ul		unsigned 32bit number
	@return true on success / false on memory allocation error
	*/
	bool put_U32(uint32_t ul);

	/**
	Insert signed 32bit integer.
	Adds an signed 32bit integer number to the payload of the packet.

	@param	l		signed 32bit number
	@return true on success / false on memory allocation error
	*/
	bool put_S32(int32_t l);

	/**
	Insert unsigned 64bit integer.
	Adds an unsigned 64bit integer number to the payload of the packet.

	@param	ull		unsigned 64bit number
	@return true on success / false on memory allocation error
	*/
	bool put_U64(uint64_t ull);

	/**
	Insert signed 64bit integer.
	Adds an signed 64bit integer number to the payload of the packet.

	@param	ll		signed 64bit number
	@return true on success / false on memory allocation error
	*/
	bool put_S64(int64_t ll);

	/**
	Insert a binary large object.
	Adds a binary object to the payload of the packet.

	@param	source		pointer to blob data
	@param	length		size of the blob in bytes
	@return true on success / false on memory allocation error
	*/
	bool put_Blob(uint8_t source[], uint32_t length);

	/**
	Reserve space.
	Creates a memory region in the payload of the packet.

	@param	length		number of bytes to reserve
	@param  fill		fill the reserved memory with a unsigned char value
	@param  c			unsigned char value to use for filling the memory area
	@return pointer to the reserved memory area or NULL if memory allocation failed
	*/
	uint8_t* reserve(uint32_t length, bool fill = false, unsigned char c = 0);

	/**
	Consume space.
	Consume a memory region in the payload of the packet. consume is the counter-part of reserve.

	@param length		number of bytes to consume
	@return pointer to the consumed memory area
	*/
	uint8_t* consume(uint32_t length);

	/**
	Clear payload data.
	Remove payload data from packet
	*/
	void clear();

	/**
	Rewind the data access pointer.
	Sets the pointer for the next "get_" operation to the beginning of the object.
	*/
	void rewind();

	/**
	Extract NULL terminated string.
	Return a NULL terminated string from the payload. The internal payload pointer will be moved
	to the end of the string for the next "extract" call.

	@return pointer to string at current payload position
	*/
	const char* get_String();

	/**
	Extract unsigned 8bit integer.
	Return unsigned 8bit integer at the current payload position pointer. The internal payload pointer will be incremented
	by the size of the binary representation of the number for the next "extract" call.

	@return unsigned 8bit integer at current payload position
	*/
	uint8_t get_U8();

	/**
	Extract unsigned 16bit integer.
	Return unsigned 16bit integer at the current payload position pointer. The internal payload pointer will be incremented
	by the size of the binary representation of the number for the next "extract" call.

	@return unsigned 16bit integer at current payload position
	*/
	uint16_t get_U16();

	/**
	Extract signed 16bit integer.
	Return signed 16bit integer at the current payload position pointer. The internal payload pointer will be incremented
	by the size of the binary representation of the number for the next "extract" call.

	@return signed 16bit integer at current payload position
	*/
	int16_t get_S16();

	/**
	Extract unsigned 32bit integer.
	Return unsigned 32bit integer at the current payload position pointer. The internal payload pointer will be incremented
	by the size of the binary representation of the number for the next "extract" call.

	@return unsigned 32bit integer at current payload position
	*/
	uint32_t get_U32();

	/**
	Extract signed 32bit integer.
	Return signed 32bit integer at the current payload position pointer. The internal payload pointer will be incremented
	by the size of the binary representation of the number for the next "extract" call.

	@return signed 32bit integer at current payload position
	*/
	int32_t get_S32();

	/**
	Extract unsigned 64bit integer.
	Return unsigned 64bit integer at the current payload position pointer. The internal payload pointer will be incremented
	by the size of the binary representation of the number for the next "extract" call.

	@return unsigned 64bit integer at current payload position
	*/
	uint64_t get_U64();

	/**
	Extract signed 64bit integer.
	Return signed 64bit integer at the current payload position pointer. The internal payload pointer will be incremented
	by the size of the binary representation of the number for the next "extract" call.

	@return signed 64bit integer at current payload position
	*/
	int64_t get_S64();

	/**
	Extract binary large object.
	Copy "length" bytes from the current payload position to "dest". The internal payload pointer will be incremented
	by the "length" for the next "extract" call.

	@param dest	pointer to destination buffer (must have a size of at least "length" bytes)
	@param length	number of bytes to copy
	@return true on success
	*/
	bool get_Blob(uint8_t dest[], uint32_t length);

	/**
	Set the user-defined client id.
	Add a user-defined client id to the packet header

	@param	oid	owner id
	*/
	void setClientID(uint16_t oid);

	/**
	Get the user-defined client id.
	Read the user-defined client id from the packet header

	@return owner id
	*/
	uint16_t getClientID();

	/**
	Check for end of payload data.
	Calling the "extract" functions shifts the payload pointer. This function checks
	if there is still data available within the payload.

	@return true - if there is no more data in the payload available
	*/
	bool eop();

	/**
	Freeze message packet.
	Compute header checksums and freeze the current packet state.
	*/
	void freeze();

	/**
	Get pointer to packet data.
	Returns a pointer to the packet header data

	@return pointer to packet header
	*/
	uint8_t* getPacket();

	/**
	Get packet length.
	Returns the total size of the packet (header + payload)

	@return total size of the packet
	*/
	uint32_t getPacketLength();

	/**
	Get pointer to payload data.
	Returns a pointer to the packets packets payload data

	@return pointer to payload data
	*/
	uint8_t* getPayload();

	/**
	Get payload length.
	Return the size of the packets payload

	@return payload size
	*/
	uint32_t getPayloadLength();

	/**
	Get unique message id.
	Returns the unique message id of the packet

	@return unique id of the packet
	*/
	uint32_t getUID();

	/**
	Get message id.
	Returns the user-defined message id

	@return message id
	*/
	uint16_t getMsgID();

	/**
	Get message type.
	Return the user-defined message type

	@return message type
	*/
	uint16_t getType();

	/**
	Get header checksum.
	Return the header checksum of the packet

	@return header checksum
	*/
	uint32_t getCheckSum();

	/**
	Get payload checksum.
	Return the payload checksum of the packet

	@return payload checksum
	*/
	uint32_t getPayloadCheckSum();

	/**
	Disable the payload checksum.
	The payload checksum will not be generated for this packet
	*/
	void disablePayloadCheckSum();

	/**
	Get protocol version.
	Return the user defined protocol version

	@return protocol version
	*/
	uint16_t getProtocolVersion();

	/**
	Set protocol version.
	Sets the user defined protocol version

	@param version protocol version
	*/
	void setProtocolVersion(uint16_t version);

	/**
	Set message id.
	Sets the user-defined message id

	@param msgid message id
	*/
	void setMsgID(uint16_t msgid);

	/**
	Set message type.
	Sets the user-defined message type

	@param type message type
	*/
	void setType(uint16_t type);

	/**
	Compress packet.
	Compress the payload of the packet

	@param level compression level (1 - 9)
	@return true on success
	*/
	bool compress(int level);

	bool isCompressed();

	/**
	Uncompress packet.
	Uncompress the payload of the packet

	@return true on success
	*/
	bool uncompress();

	void print();

	/**
	Write packet to socket.
	Writes the packet data to a filedescriptor

	@param	fd		filedescriptor of the socket
	@param	timeout_ms	write operation timeout in milliseconds
	*/
	bool write(int fd, int timeout_ms = 3000);

	/**
	Copy packet.
	copies the contents of packet p into this one

	@param p		packet to copy
	*/
	void copy(MsgPacket* p);

	inline int remaining() const {
		return m_usage - m_readposition;
	}
	/**
	Receive packet from socket.
	Create a new packet from incoming socket data

	@param	fd		filedescriptor of the socket
	@param	timeout_ms	read operation timeout in milliseconds
	@return pointer to new packet or NULL on timeout
	*/
	static MsgPacket* read(int fd, int timeout_ms = 3000);

	/**
	Receive packet from socket.
	Reads into a packet from incoming socket data

	@param	fd		filedescriptor of the socket
	@param  p		pointer to packet
	@param	timeout_ms	read operation timeout in milliseconds
	@return true on success, otherwise false
	*/
	static bool read(int fd, MsgPacket* p, int timeout_ms = 3000);

	/**
	Receive packet from socket.
	Reads into a packet from incoming socket data

	@param	fd			filedescriptor of the socket
	@param	closed		set to true if connection has been closed
	@param  p			pointer to packet
	@param	timeout_ms	read operation timeout in milliseconds
	@return true on success, otherwise false
	*/
	static bool read(int fd, bool& closed, MsgPacket* p, int timeout_ms = 3000);

	/**
	Receive packet from socket.
	Create a new packet from incoming socket data

	@param	fd			filedescriptor of the socket
	@param	closed		set to true if connection has been closed
	@param	timeout_ms	read operation timeout in milliseconds
	@return pointer to new packet or NULL on timeout
	*/
	static MsgPacket* read(int fd, bool& closed, int timeout_ms = 3000);

	static bool readstream(std::istream& in, MsgPacket& p);

	enum {
		HeaderLength = 32,						/*!< Length (in bytes) of a packet header. */
		CheckSumPos = 28,						/*!< Checksum position (uint32_t) within the header data. */
		UncompressedPayloadLengthPos = 24,		/*!< uncompressed payload length position (uint32_t). only compressed packets have this value set. */
		PayloadLengthPos = 20,					/*!< payload length position (uint32_t). */
		PayloadCheckSumPos = 16,				/*!< checksum position of the payload (uint32_t). */
		ProtocolVersionPos = 14,				/*!< protocol-version position (uint16_t). */
		ClientIDPos = 12,						/*!< owner-id position (uint16_t). */
		TypePos = 10,							/*!< message-type position (uint16_t). */
		MsgIDPos = 8,							/*!< message-id position (uint32_t). */
		UIDPos = 4,								/*!< message-uid position (uint32_t). */
		SyncPos = 0								/*!< sync-mark position (uint32_t). */
	};

protected:

	void Init(uint16_t msgid, uint16_t type = 0, uint32_t uid = 0);

	/**
	Set unique message id.
	Sets the unique message id of the packet

	@param uid unique id of the packet
	*/
	void setUID(uint32_t uid);

	/**
	Compute a CRC32 checksum.

	@param  buf		pointer to data array
	@param  size    size of array in bytes
	@return 32bit crc
	*/
	static uint32_t crc32(const uint8_t* buf, int size);

	static int read(int fd, uint8_t* data, int datalen, int timeout_ms);

private:

	template<typename T>
	void writePacket(int pos, T value) {
		if(sizeof(T) > 4) {
			memcpy((void*)&m_packet[pos], (void*)&value, sizeof(T));
		}
		else {
			*(T*)&m_packet[pos] = value;
		}
	}

	template<typename T>
	T readPacket(int pos) {
		if(sizeof(T) > 4) {
			T r;
			memcpy((void*)&r, (void*)&m_packet[pos], sizeof(T));
			return r;
		}

		return *(T*)&m_packet[pos];
	}

	bool checkPacketSize(uint32_t bytes);

	static uint32_t globalUID;
	static uint32_t crc32_tab[];
	static std::mutex uidmutex;

	uint8_t* m_packet;
	uint32_t m_size;
	uint32_t m_usage;
	uint32_t m_readposition;

	bool m_freezed;
	bool m_payloadchecksum;

	enum {
		InitialPacketSize = 128,
		IncrementPacketSize = 512
	};
};

inline std::ostream& operator<<(std::ostream& out, MsgPacket& p) {
	return out.write((const char*)p.getPacket(), p.getPacketLength());
}

inline std::istream& operator>>(std::istream& in, MsgPacket& p) {
	MsgPacket::readstream(in, p);
	return in;
}

/*
@startuml

note left of MsgPacket
 <b>Message Packet</b>
 The primary interface for messages passed
 between communication endpoints.

 <b>Sources:</b>
 libmsgexchange/include/msgpacket.h
 libmsgexchange/src/msgpacket.cpp
end note

class MsgPacket {
.. data putters ..
+bool put_String(const char* string)
+bool put_U8(uint8_t c)
+bool put_U16(uint16_t us)
+bool put_S16(int16_t s)
+bool put_U32(uint32_t ul)
+bool put_S32(int32_t l)
+bool put_U64(uint64_t ull)
+bool put_S64(int64_t ll)
+bool put_Blob(uint8_t source[], uint32_t length)
.. data getters ..
+const char* get_String()
+uint8_t get_U8()
+uint16_t get_U16()
+int16_t get_S16()
+uint32_t get_U32()
+int32_t get_S32()
+uint64_t get_U64()
+int64_t get_S64()
+bool get_Blob(uint8_t dest[], uint32_t length)
.. memory allocation ..
+uint8_t* reserve(uint32_t length, bool fill, unsigned char c)
+uint8_t* consume(uint32_t length)
+void clear()
.. compression ..
+bool compress(int level)
+bool uncompress()
.. transport ..
+{static} MsgPacket* read(int fd, bool& closed, int timeout_ms)
+bool write(int fd, int timeout_ms)
--
-{static} uint32_t globalUID
-uint8_t* m_packet;
-uint32_t m_size;
-uint32_t m_usage;
-uint32_t m_readposition;
}
@enduml
*/

#endif // MSGPACKET_H
