#ifndef SESSIONLISTENER_H
#define SESSIONLISTENER_H

#include "msgpacket.h"

class SessionListener {
public:

    virtual ~SessionListener() = default;

    virtual void onNotification(MsgPacket* p) = 0;

    virtual void onDisconnect() = 0;

};

#endif // SESSIONLISTENER_H
