/*
    libmsgexchange - Client / Server Message Exchange Library
    Copyright (C) 2012 BMS Informationstechnologie GmbH
*/

#include <unistd.h>

#include "msgthread.h"
#include "msgthreadqueue.h"

MsgThread::MsgThread() : m_running(false), m_queue(NULL), m_id(0) {
	m_sleeping = false;
	m_stopped = true;
}

MsgThread::MsgThread(MsgThreadQueue* q) : m_running(false), m_queue(q), m_id(0) {
	m_sleeping = false;
	m_stopped = true;

	if(m_queue != NULL) {
		m_queue->add(this);
	}
}

MsgThread::~MsgThread() {
}

bool MsgThread::Start() {
	return Start(std::bind(&MsgThread::Run, this));
}

bool MsgThread::Start(std::function<void()> fn) {
	std::lock_guard<std::mutex> lock(m_mutex);

	if(!m_stopped) {
		return false;
	}

	m_stopped = false;

	m_startup.Reset();
	m_worker = std::thread(&MsgThread::ThreadWorker, this, fn);
	m_startup.Wait();
	return true;
}

bool MsgThread::Stop() {
	std::lock_guard<std::mutex> lock(m_mutex);

	if(m_stopped) {
		return false;
	}

	// wake up maybe sleeping thread
	m_running = false;
	Awake();

	m_worker.join();
	m_stopped = true;

	return true;
}

bool MsgThread::IsRunning() const {
	return m_running;
}

bool MsgThread::IsStopped() const {
	return m_stopped;
}

void MsgThread::ThreadWorker(std::function<void()> runnable) {
	m_running = true;
	m_startup.Signal();

	runnable();

	m_running = false;
}

void MsgThread::Run() {
}

bool MsgThread::Sleep(int timeout_ms) {
	bool rc = false;

	if(!m_sleeping) {
		m_sleeping = true;

		rc = m_wakeup.Wait(timeout_ms);
		m_sleeping = false;
	}

	return rc;
}

bool MsgThread::Sleep() {
	return MsgThread::Sleep(0);
}

bool MsgThread::Awake() {
	if(!m_sleeping) {
		return false;
	}

	m_wakeup.Signal();
	return true;
}

bool MsgThread::IsSleeping() const {
	return m_sleeping;
}

void MsgThread::SetID(uint64_t id) {
	m_id = id;
}

uint64_t MsgThread::GetID() const {
	return m_id;
}
