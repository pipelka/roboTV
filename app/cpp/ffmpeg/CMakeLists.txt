if( "${CMAKE_ANDROID_ARCH_ABI}" STREQUAL "x86" )
    set(FFMPEG_LIBDIR "${CMAKE_SOURCE_DIR}/ffmpeg/libs/x86")
else()
    set(FFMPEG_LIBDIR "${CMAKE_SOURCE_DIR}/ffmpeg/libs/armeabi-v7a")
endif()


add_library(avcodec SHARED IMPORTED)
add_dependencies(avcodec ffmpeg_native)
set_property(TARGET avcodec PROPERTY IMPORTED_LOCATION ${FFMPEG_LIBDIR}/libavcodec.so)

add_library(avutil SHARED IMPORTED)
set_property(TARGET avutil PROPERTY IMPORTED_LOCATION ${FFMPEG_LIBDIR}/libavutil.so)

add_library(swresample SHARED IMPORTED)
set_property(TARGET swresample PROPERTY IMPORTED_LOCATION ${FFMPEG_LIBDIR}/libswresample.so)

add_library(avresample SHARED IMPORTED)
set_property(TARGET avresample PROPERTY IMPORTED_LOCATION ${FFMPEG_LIBDIR}/libavresample.so)


add_library(ffmpeg SHARED ffmpeg_jni.cc)
target_include_directories(ffmpeg PRIVATE ./include)
target_link_libraries(ffmpeg avcodec avresample avutil swresample log)
