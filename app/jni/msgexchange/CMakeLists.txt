cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_ZLIB=1 -std=c++11 -pthread -frtti")

add_library(msgexchange SHARED
	src/msgcondition.cpp
	src/msgconnection.cpp
	src/msgpacket.cpp
	src/msgsession.cpp
	src/msgthread.cpp
	src/msgthreadqueue.cpp
	src/os-config.cpp)

target_include_directories(msgexchange PUBLIC ./include)
target_link_libraries(msgexchange z)
