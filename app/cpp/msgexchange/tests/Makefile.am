AM_CXXFLAGS = -std=c++0x

noinst_PROGRAMS = \
	client \
	listener \
	server \
	unittest

if JSON
noinst_PROGRAMS += \
	json \
	jsonserver
endif

client_SOURCES = \
	client.cpp

client_LDADD = \
	../src/libmsgexchange_static.la

listener_SOURCES = \
	listener.cpp

listener_LDADD = \
	../src/libmsgexchange_static.la

server_SOURCES = \
	server.cpp

server_LDADD = \
	../src/libmsgexchange_static.la

if JSON
json_SOURCES = \
	json.cpp

json_LDADD = \
	../src/libmsgexchange_static.la

jsonserver_SOURCES = \
	jsonserver.cpp

jsonserver_LDADD = \
	../src/libmsgexchange_static.la
endif

unittest_SOURCES = \
	unittest.cpp

unittest_LDADD = \
	../src/libmsgexchange_static.la \
	$(GTEST_LIBS) \
	$(PTHREAD_LIBS) \
	-lgcov

INCLUDES = \
	-I$(top_srcdir)/include
