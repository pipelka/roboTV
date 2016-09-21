AM_CXXFLAGS = -std=c++0x

SOURCES = \
	$(JSONSOURCES) \
	msgcondition.cpp \
	msgconnection.cpp \
	msghandler.cpp \
	msghandlerbase.cpp \
	msgpacket.cpp \
	msgserver.cpp \
	msgsession.cpp \
	msgthread.cpp \
	msgthreadqueue.cpp \
	os-config.cpp \
	os-config.h

if JSON
SOURCES += \
	json/json.c \
	json/json.h \
	msgjson.cpp \
	msghandlerjson.cpp
endif

lib_LTLIBRARIES = libmsgexchange.la

libmsgexchange_la_SOURCES = \
	$(SOURCES)

libmsgexchange_la_LDFLAGS = \
	-release $(LT_RELEASE) \
	-version-info $(LT_CURRENT):$(LT_REVISION):$(LT_AGE) \
	$(MINGW32FLAGS)

libmsgexchange_la_CPPFLAGS = \
	-I$(top_srcdir)/include

libmsgexchange_la_LIBADD = \
	$(ZLIB_LIBS) \
	$(PTHREAD_LIBS) \
	$(WS2_LIBS)



noinst_LTLIBRARIES = libmsgexchange_static.la

libmsgexchange_static_la_SOURCES = \
	$(SOURCES)

libmsgexchange_static_la_LIBADD = \
	$(ZLIB_LIBS) \
	$(PTHREAD_LIBS) \
	$(WS2_LIBS)

libmsgexchange_static_la_CPPFLAGS = \
	-I$(top_srcdir)/include
