/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

/** \file msgjson.h
	Header file for the JSON support functions.
*/

#ifndef MSGJSON_H
#define MSGJSON_H

#include <string>
#include <stdint.h>

class MsgPacket;

/**
Create MsgPacket from JSON string
Converts the given JSON string to a message packet.

@param	json   JSON string
@return pointer to new message packet
*/
MsgPacket* MsgPacketFromJSON(const std::string& json, uint16_t msgid);

/**
Create a JSON string from a message packet,
Converts the given a message packet with help of a format string to a JSON string.

@param  p            Pointer to message packet
@param	jsonformat   JSON format string
@return JSON representation of the packet
*/
std::string MsgPacketToJSON(MsgPacket* p, const std::string& jsonformat);

#endif // MSGJSON_H
