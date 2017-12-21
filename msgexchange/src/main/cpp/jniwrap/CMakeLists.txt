cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_ZLIB=1 -std=c++11 -pthread -frtti -fexceptions")


add_library(msgexchange_jni SHARED
	jniwrap.cpp
	jniwrap.h
	sessionlistener.h)

target_link_libraries(msgexchange_jni msgexchange log)
