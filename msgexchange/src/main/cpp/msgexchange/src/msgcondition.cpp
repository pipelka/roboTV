/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include "msgcondition.h"

MsgCondition::MsgCondition() : m_signaled(false) {
}

MsgCondition::~MsgCondition() {
}

bool MsgCondition::Wait(int timeout_ms) {
	std::unique_lock<std::mutex> lock(m_lock);

	if(!m_signaled) {
		if(timeout_ms > 0) {
			m_cond.wait_for(lock, std::chrono::milliseconds(timeout_ms), [this]() {
				return m_signaled;
			});
		}
		else {
			m_cond.wait(lock, [this]() {
				return m_signaled;
			});
		}
	}

	bool rc = m_signaled;
	m_signaled = false;

	return rc;
}

void MsgCondition::Signal() {
	std::lock_guard<std::mutex> lock(m_lock);
	m_signaled = true;
	m_cond.notify_all();
}

void MsgCondition::Reset() {
	std::lock_guard<std::mutex> lock(m_lock);
	m_cond.notify_all();
	m_signaled = false;
}
