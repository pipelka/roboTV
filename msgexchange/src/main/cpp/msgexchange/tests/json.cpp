#include "msgjson.h"
#include "msgpacket.h"
#include <iostream>

int main(int agrc, char* argv[]) {
	std::string json =
	    "{\n"
	    "\"msgid\": 23,\n"
	    "\"type\": 2,\n"
	    "\"packet\":\n"
	    "  [\n"
	    "  { \"name\": \"param1\", \"type\": \"string\", \"value\": \"string1\" },\n"
	    "  { \"name\": \"param2\", \"type\": \"uint32\", \"value\": 3433 },\n"
	    "  { \"name\": \"param3\", \"type\": \"uint8\", \"value\": 255 },\n"
	    "  { \"name\": \"param4\", \"type\": \"string\", \"value\": \"string1\" },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 111 },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 2 }\n"
	    "  ]\n"
	    "}";

	std::string jsonformat =
	    "{\n"
	    "\"packet\":\n"
	    "  [\n"
	    "  { \"name\": \"param1\", \"type\": \"string\", \"value\": \"\" },\n"
	    "  { \"name\": \"param2\", \"type\": \"uint32\", \"value\": 0 },\n"
	    "  { \"name\": \"param3\", \"type\": \"uint8\", \"value\": 0 },\n"
	    "  { \"name\": \"param4\", \"type\": \"string\", \"value\": \"\" },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 0 },\n"
	    "  { \"name\": \"encoderid\", \"type\": \"uint32\", \"value\": 0 }\n"
	    "  ]\n"
	    "}";

	std::cout << "JSON (original): " << json << std::endl;
	MsgPacket* p = MsgPacketFromJSON(json, 11);

	p->print();

	std::string result = MsgPacketToJSON(p, jsonformat);
	std::cout << "JSON: " << result << std::endl;

	delete p;
	return 0;
}
