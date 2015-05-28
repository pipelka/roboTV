#include "msgthreadqueue.h"
#include "msgthread.h"

MsgThreadQueue::MsgThreadQueue() {
	Start();
}

MsgThreadQueue::~MsgThreadQueue() {
	StopAll();
	Stop();
}

bool MsgThreadQueue::add(MsgThread* t) {
	std::lock_guard<std::recursive_mutex> lock(m_mutex);

	t->m_queue = this;
	insert(t);

	return true;
}

bool MsgThreadQueue::remove(MsgThread* t) {
	Awake();
	return true;
}

void MsgThreadQueue::StopAll() {
	std::lock_guard<std::recursive_mutex> lock(m_mutex);

	auto t = begin();

	for(; t != end();) {
		if(*t != NULL) {
			(*t)->Stop();
			delete(*t);
			t = erase(t);
		}
		else {
			t++;
		}
	}
}

bool MsgThreadQueue::Run(MsgThread* t) {
	if(t->Start()) {
		add(t);
		return true;
	}

	return false;
}

bool MsgThreadQueue::RunFunction(std::function<void()> fn) {
	MsgThread* t = new MsgThread();

	if(t->Start(fn)) {
		add(t);
		return true;
	}

	return false;
}

void MsgThreadQueue::Run() {
	while(IsRunning() || !empty()) {
		Sleep(100);

		// stop & remove all finished threads
		{
			std::lock_guard<std::recursive_mutex> lock(m_mutex);

			auto t = begin();

			for(; t != end();) {
				if(*t != NULL && !(*t)->IsRunning()) {
					(*t)->Stop();
					delete(*t);
					t = erase(t);
				}
				else {
					t++;
				}
			}
		}
	}
}

MsgThread* MsgThreadQueue::FindByID(uint64_t id) {
	std::lock_guard<std::recursive_mutex> lock(m_mutex);

	for(auto i = begin(); i != end(); i++) {
		if((*i)->GetID() == id) {
			return (*i);
		}
	}

	return NULL;
}

bool MsgThreadQueue::empty() {
	std::lock_guard<std::recursive_mutex> lock(m_mutex);
	return std::set<MsgThread*>::empty();
}

void MsgThreadQueue::foreach(std::function<void(MsgThread*)> fn) {
	std::lock_guard<std::recursive_mutex> lock(m_mutex);

	if(!IsRunning() || IsStopped()) {
		return;
	}

	for(auto i : (std::set<MsgThread*>)(*this)) {
		fn(i);
	}
}
