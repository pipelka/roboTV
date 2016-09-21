#ifndef SESSION_H
#define SESSION_H

#include <msgsession.h>
#include "sessionlistener.h"

class Session : public MsgSession {
public:

    Session() = default;

    virtual ~Session() = default;

    void setCallback(SessionListener* listener) {
        m_callback = listener;
    }

protected:

    void OnNotification(MsgPacket* notification) {
        if(m_callback) {
            m_callback->onNotification(notification);
        }
    }

    void OnDisconnect() {
        if(m_callback) {
            m_callback->onDisconnect();
        }
    }

    void OnReconnect() {
        if(m_callback) {
            m_callback->onReconnect();
        }
    }

private:

    SessionListener* m_callback = nullptr;
};

#endif // SESSION_H
