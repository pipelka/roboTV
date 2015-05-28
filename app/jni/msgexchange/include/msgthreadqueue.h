/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/


/** \file msgthreadqueue.h
	Header file for the MsgThreadQueue class.
	This include file defines the MsgThreadQueue class
*/

#ifndef MSGTHREADQUEUE_H
#define MSGTHREADQUEUE_H

#include <thread>
#include <set>
#include <mutex>

#include "msgcondition.h"
#include "msgthread.h"

/**
 * 	@short Thread Queue.
 *
 *  Manages the lifetime of a set of threads.
 *  Threads running in the context of a queue will delete themselves, the
 *  thread queue is able to wait until all thread have finished.
 */

class MsgThreadQueue : protected std::set<MsgThread*>, public MsgThread {
public:

	/**
	 * ThreadQueue constructor.
	 *
	 * Constructe the ThreadQueue class object.
	 */
	MsgThreadQueue();

	/**
	 * ThreadQueue destructor.
	 *
	 * Waits until all queued threads have finished.
	 */
	~MsgThreadQueue();

	/**
	 * Queue and run thread.
	 *
	 * Adds the thread object to the queue and calls Start().
	 *
	 * @return true on successfully starting the thread, otherwise false.
	 */
	bool Run(MsgThread* t);

	/**
	 * Queue and run function.
	 *
	 * Creates a new MsgThread object from the given function, queues it
	 * and calls Run()
	 *
	 * @return true on successfully starting the thread, otherwise false.
	 */
	bool RunFunction(std::function<void()> fn);

	/**
	 * Stop all queued threads.
	 *
	 * Schedules all queued threads to stop. This call will return immediately, stopping
	 * of threads will be done asynchronously. To make sure all threads have been stopped,
	 * call Stop() on the ThreadQueue object.
	 */
	void StopAll();

	/**
	 * Find thread by id.
	 *
	 * Each threads may have set an id (MsgThread::SetID()). This function returns the first
	 * threads with a matching id.
	 *
	 * @return pointer to matching MsgThread object
	 */
	MsgThread* FindByID(uint64_t id);

	/**
	 * Check if queue is empty.
	 *
	 * return true if there isn't any thread in the queue, otherwise falsde.
	 */
	bool empty();

	/**
	 * Call a function for each queued thread.
	 *
	 * The given function will be called for each queued (and still running) thread.
	 */
	void foreach(std::function<void(MsgThread*)> fn);

protected:

	bool add(MsgThread* t);

	bool remove(MsgThread* t);

	void Run();

	friend class MsgThread;

private:

	std::recursive_mutex m_mutex;

	std::set<MsgThread*> m_trash;
};

/*
@startuml

MsgThreadQueue "1" *-- "n" MsgThread : holds

class MsgThreadQueue {
+MsgThreadQueue()
+~MsgThreadQueue()
+bool Run(MsgThread* t)
+bool RunFunction(std::function<void()> fn)
+void StopAll()
+MsgThread* FindByID(uint64_t id)
+bool empty()
+void foreach(std::function<void(MsgThread*)> fn)
#bool add(MsgThread* t)
#bool remove(MsgThread* t)
#void Run()
-std::recursive_mutex m_mutex
-std::set<MsgThread*> m_trash
}

@enduml
*/

#endif // MSGTHREADQUEUE_H
