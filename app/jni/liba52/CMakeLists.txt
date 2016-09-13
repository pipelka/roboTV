cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

set(A52PATH ../external/a52dec-0.7.4)

# now build app's shared lib
add_library(a52 SHARED
    ${A52PATH}/liba52/bitstream.c
    ${A52PATH}/liba52/imdct.c
    ${A52PATH}/liba52/bit_allocate.c
    ${A52PATH}/liba52/parse.c
    ${A52PATH}/liba52/downmix.c)

target_include_directories(a52 PUBLIC ${A52PATH}/include PRIVATE ./a52config)

# add lib dependencies
target_link_libraries(a52 m)
