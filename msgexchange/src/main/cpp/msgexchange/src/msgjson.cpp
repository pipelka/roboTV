/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include "msgjson.h"
#include "msgpacket.h"
#include "json/json.h"

#include <stdlib.h>
#include <stdio.h>
#include <iostream>
#include <sstream>

class ParserObject {
public:

	ParserObject() {
	}

	virtual ~ParserObject() {
	}

	bool Parse(const std::string& json, uint16_t& msgid, uint16_t& type) {
		json_settings settings;
		memset(&settings, 0, sizeof(json_settings));
		char error_buf[512];

		json_value* object = json_parse_ex(&settings, json.c_str(), error_buf);

		if(object == NULL) {
			std::cerr << error_buf << std::endl;
			return false;
		}

		// check if its a valid object
		if(object->type != json_object) {
			json_value_free(object);
			return false;
		}

		// get packet array
		json_value packet = (*object)["packet"];

		// set msgid (if available)
		json_value id = (*object)["msgid"];

		if(id.type != json_none) {
			msgid = (long)id;
		}

		// set type (if available)
		json_value t = (*object)["type"];

		if(t.type != json_none) {
			type = (long)t;
		}

		// iterate through array
		for(int i = 0; ; i++) {
			json_value o = packet[i];

			if(o.type != json_object) {
				break;
			}

			OnPacketData(o);
		}

		json_value_free(object);
		return true;
	}

protected:

	virtual void OnPacketData(json_value& o) {
	}
};

class ParserCreatePacket : public ParserObject {
public:

	ParserCreatePacket(MsgPacket* p) : m_packet(p) {
	}

protected:

	void OnPacketData(json_value& o) {
		if(strcmp(o["type"], "string") == 0) {
			m_packet->put_String(o["value"]);
		}
		else {
			if(strcmp(o["type"], "uint8") == 0) {
				m_packet->put_U8((long)o["value"]);
			}
			else if(strcmp(o["type"], "uint16") == 0) {
				m_packet->put_U16((long)o["value"]);
			}
			else if(strcmp(o["type"], "uint32") == 0) {
				m_packet->put_U32((long)o["value"]);
			}
			else if(strcmp(o["type"], "uint64") == 0) {
				m_packet->put_U64((long)o["value"]);
			}
			else if(strcmp(o["type"], "sint16") == 0) {
				m_packet->put_S16((long)o["value"]);
			}
			else if(strcmp(o["type"], "sint32") == 0) {
				m_packet->put_S32((long)o["value"]);
			}
			else if(strcmp(o["type"], "sint64") == 0) {
				m_packet->put_S64((long)o["value"]);
			}
		}
	}

	MsgPacket* m_packet;
};

class ParserCreateJSON : public ParserObject {
public:

	ParserCreateJSON(MsgPacket* p) : m_packet(p) {
	}

	std::string m_json;

protected:

	void OnPacketData(json_value& o) {
		std::stringstream str;
		str.flags(std::ios::boolalpha);

		m_json += "  { \"name\": \"";
		m_json += (const char*)o["name"];
		m_json += "\", ";
		m_json += "\"type\": \"";
		m_json += (const char*)o["type"];
		m_json += "\", ";
		m_json += "\"value\": ";

		if(strcmp(o["type"], "string") == 0) {
			m_json += "\"";
			m_json += m_packet->get_String();
			m_json += "\"";
		}
		else {
			if(strcmp(o["type"], "uint8") == 0) {
				str << (int)m_packet->get_U8();
			}
			else if(strcmp(o["type"], "uint16") == 0) {
				str << m_packet->get_U16();
			}
			else if(strcmp(o["type"], "uint32") == 0) {
				str << m_packet->get_U32();
			}
			else if(strcmp(o["type"], "uint64") == 0) {
				str << m_packet->get_U32();
			}
			else if(strcmp(o["type"], "sint16") == 0) {
				str << m_packet->get_U32();
			}
			else if(strcmp(o["type"], "sint32") == 0) {
				str << m_packet->get_U32();
			}
			else if(strcmp(o["type"], "sint64") == 0) {
				str << m_packet->get_U32();
			}

			std::string v;
			str >> v;
			m_json += v;
		}

		m_json += " },\n";
	}

	MsgPacket* m_packet;
};

MsgPacket* MsgPacketFromJSON(const std::string& json, uint16_t msgid) {
	// create packet
	uint16_t type = 0;
	uint16_t req_msgid;
	MsgPacket* p = new MsgPacket(msgid);

	// create parser
	ParserCreatePacket parser(p);

	// parse JSON data
	if(!parser.Parse(json, req_msgid, type)) {
		delete p;
		return NULL;
	}

	if(req_msgid > 0) {
		msgid = req_msgid;
	}

	p->setMsgID(msgid);
	p->setType(type);

	return p;
}

std::string MsgPacketToJSON(MsgPacket* p, const std::string& jsonformat) {
	uint16_t msgid = 0;
	uint16_t type = 0;
	char buffer[20];

	// create parser
	ParserCreateJSON parser(p);
	parser.m_json = "{\n";

	snprintf(buffer, sizeof(buffer), "\"msgid\": %i,\n", p->getMsgID());
	parser.m_json += buffer;

	snprintf(buffer, sizeof(buffer), "\"type\": %i,\n", p->getType());
	parser.m_json += buffer;

	parser.m_json += "\"packet\":\n  [\n";

	// parse packet data
	if(!parser.Parse(jsonformat, msgid, type)) {
		std::cerr << "parsing json failed!" << std::endl;
		return "";
	}

	if(parser.m_json.size() > 2) {
		parser.m_json = parser.m_json.substr(0, parser.m_json.size() - 2);
	}

	parser.m_json += "\n  ]\n}";

	return parser.m_json;
}
