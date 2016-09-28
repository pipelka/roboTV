cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_ZLIB=1 -std=c++11 -pthread -frtti -fexceptions")


add_library(jniwrap SHARED
	jniwrap.cpp
	jniwrap.h
	decoderjni.cpp
	sessionlistener.h
	ac3decoder.cpp
	mpadecoder.cpp)

target_link_libraries(jniwrap msgexchange a52 mad log)
