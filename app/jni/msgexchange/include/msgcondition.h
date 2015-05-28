/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgcondition.h
	Header file for the MsgCondition class.
	This include file defines the MsgCondition class
*/

#ifndef MSGCONDITION_H
#define MSGCONDITION_H

#include <mutex>
#include <condition_variable>

/**
	@short Condition variable

	Convenience wrapper for the C++ condition_variable.
*/

class MsgCondition {
public:

	MsgCondition();

	virtual ~MsgCondition();

	/**
	Wait for a signal.
	Waits until an other thread emits a Signal() or a timeout event occurs.

	@return true on Signal() recpetion otherwise the call timed out
	*/

	bool Wait(int timeout_ms = 0);

	/**
	Signal a waiting thread.
	Releases a Wait() operation of an other thread.
	*/
	void Signal();

	void Reset();

private:

	std::condition_variable m_cond;

	std::mutex m_lock;

	bool m_signaled;

};

/*
@startuml

class MsgCondition {
+MsgCondition()
{abstract}+~MsgCondition()
+bool Wait(int timeout_ms = 0)
+void Signal()
-std::condition_variable m_cond
-std::mutex m_lock
-bool m_signaled
}

@enduml
*/

#endif // MSGCONDITION_H
