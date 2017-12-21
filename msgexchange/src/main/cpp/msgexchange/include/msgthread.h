/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgthread.h
	Header file for the MsgThread class.
	This include file defines the MsgThread class
*/

#ifndef MSGTHREAD_H
#define MSGTHREAD_H

#include <stdint.h>
#include <thread>
#include <atomic>
#include <mutex>

#include "msgcondition.h"

class MsgThreadQueue;

/**
	@short Threading class

	Package specific threading class.
*/

class MsgThread {
public:

	/**
	MsgThread constructor.
	Creates an object that may run in a separate thread.
	*/
	MsgThread();

	MsgThread(MsgThreadQueue* q);

	/**
	MsgThread destructor.
	Destroys the MsgThread object.
	*/
	virtual ~MsgThread();

	/**
	Start the worker thread.
	Starts the separate thread and executes the Run() member function.

	@return true on success
	*/
	virtual bool Start();

	/**
	Start a worker thread.
	Starts the separate thread and executes the provided function.

	@return true on success
	*/
	virtual bool Start(std::function<void()> fn);

	/**
	Tries to stop the worker thread.
	Signals the worker thread to stop.

	@return true on success
	*/
	virtual bool Stop();

	/**
	Check if thread is running.
	Checks if the worker thread is still executing the Run member function.
	*/
	bool IsRunning() const;

	bool IsStopped() const;

	bool Awake();

	void SetID(uint64_t id);

	uint64_t GetID() const;

	bool Sleep();

	bool Sleep(int timeout_ms);

protected:

	/**
	Worker-thread function.
	*/
	virtual void Run();

	bool IsSleeping() const;

	friend class MsgThreadQueue;

private:

	void ThreadWorker(std::function<void()> runnable);

	std::thread m_worker;

	std::mutex m_mutex;

	std::atomic<bool> m_running;

	std::atomic<bool> m_stopped;

	std::atomic<bool> m_sleeping;

	MsgCondition m_startup;

	MsgCondition m_wakeup;

	MsgThreadQueue* m_queue;

	uint64_t m_id;
};

/*
@startuml

note top of MsgThread
 <b>Threading Class</b>

 <b>Sources</b>
 libmsgexchange/include/msgthread.h
 libmsgexchange/src/msgthread.cpp
end note

class MsgThread {
+{abstract}bool Start()
+{abstract}bool Stop()
+bool IsRunning()
+bool IsStopped()
+bool Awake()
+void SetID(uint64_t id)
+uint64_t GetID() const
#{abstract}void Run()
#bool Sleep()
#bool IsSleeping() const
-bool m_running
-bool m_stopped
-bool m_sleeping
-std::thread m_worker
-std::mutex m_mutex
-MsgCondition m_startup
-MsgCondition m_wakeup
}

@enduml
*/

#endif //MSGTHREAD_H
