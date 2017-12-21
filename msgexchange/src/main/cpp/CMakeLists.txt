cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
SET(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -O3 -fomit-frame-pointer -ffunction-sections -fdata-sections")

add_subdirectory(msgexchange)
add_subdirectory(jniwrap)
