cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(LIBMADPATH ../external/libmad-0.15.1b)

if ("${TARGET_ARCH}" STREQUAL "x86")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DFPM_INTEL")
elseif ("${TARGET_ARCH}" STREQUAL "x86_64")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DFPM_64BIT")
else()
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DFPM_DEFAULT")
endif()

# now build app's shared lib
add_library(mad SHARED
	${LIBMADPATH}/version.c
	${LIBMADPATH}/fixed.c
	${LIBMADPATH}/bit.c
	${LIBMADPATH}/timer.c
	${LIBMADPATH}/stream.c
	${LIBMADPATH}/frame.c
	${LIBMADPATH}/synth.c
	${LIBMADPATH}/decoder.c
	${LIBMADPATH}/layer12.c
	${LIBMADPATH}/layer3.c
	${LIBMADPATH}/huffman.c)

target_include_directories(mad PUBLIC ${LIBMADPATH})
