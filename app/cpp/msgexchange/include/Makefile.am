msgexchangeincludedir = $(includedir)/msgexchange


msgexchangeinclude_HEADERS = \
	$(JSONHEADERS) \
	msgcallbacks.h \
	msgcondition.h \
	msgconnection.h \
	msghandler.h \
	msghandlerbase.h \
	msgpacket.h \
	msgserver.h \
	msgsession.h \
	msgthread.h \
	msgthreadqueue.h

if JSON
msgexchangeinclude_HEADERS += \
	msghandlerjson.h \
	msgjson.h
endif

EXTRA_DIST = \
	$(msgexchangeinclude_HEADERS)
