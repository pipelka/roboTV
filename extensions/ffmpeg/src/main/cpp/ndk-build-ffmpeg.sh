#!/bin/sh

cd `dirname $0`

NDK_BUILD=`which ndk-build`
NDK_PATH=`dirname $NDK_BUILD`

# remove dirs

rm -Rf libs
rm -Rf include

# checkout ffmpeg

[ -d ffmpeg ] || ( git clone git://source.ffmpeg.org/ffmpeg ffmpeg && cd ffmpeg && git checkout b96a6e2 && cd .. )

# arm64-v8a

TOOLCHAIN_PREFIX="${NDK_PATH}/toolchains/llvm/prebuilt/linux-x86_64"
SYSROOT="${TOOLCHAIN_PREFIX}/sysroot"
CROSS_PREFIX="${TOOLCHAIN_PREFIX}/bin/"

NDK_ABIARCH=aarch64-linux-android

cd ffmpeg

make clean | true
find . -name "*.o" | xargs rm > /dev/null 2>&1

./configure \
    --cross-prefix=$CROSS_PREFIX \
    --prefix=.. \
    --libdir=../libs/arm64-v8a \
    --enable-cross-compile \
    --arch=arm64 \
    --cpu=cortex-a57 \
    --target-os=android \
    --sysroot="${SYSROOT}" \
    --extra-cflags="-march=armv8-a -O3" \
    --extra-ldflags="" \
    --cc="${CROSS_PREFIX}${NDK_ABIARCH}21-clang" \
    --cxx="${CROSS_PREFIX}${NDK_ABIARCH}21-clang++" \
    --nm="${CROSS_PREFIX}${NDK_ABIARCH}-nm" \
    --ld="${CROSS_PREFIX}${NDK_ABIARCH}21-clang" \
    --ar="${CROSS_PREFIX}${NDK_ABIARCH}-ar" \
    --strip="${CROSS_PREFIX}${NDK_ABIARCH}-strip" \
    --extra-ldexeflags=-pie \
    --disable-static \
    --disable-neon \
    --enable-shared \
    --disable-doc \
    --disable-programs \
    --disable-everything \
    --disable-avdevice \
    --disable-avformat \
    --disable-swscale \
    --disable-postproc \
    --disable-avfilter \
    --disable-symver \
    --enable-avresample \
    --enable-decoder=h264 \
    --enable-decoder=aac_latm \
    --enable-decoder=ac3 \
    --enable-decoder=eac3 \
    --enable-decoder=mp3 || \
    exit 1

make -j4
make install-libs

make clean
find . -name "*.o" | xargs rm

# armeabi-v7a

NDK_ABIARCH=arm-linux-androideabi

./configure \
    --cross-prefix=$CROSS_PREFIX \
    --prefix=.. \
    --libdir=../libs/armeabi-v7a \
    --enable-cross-compile \
    --arch=arm \
    --cpu=armv7-a \
    --target-os=android \
    --sysroot="${SYSROOT}" \
    --extra-cflags="-march=armv7-a -mfloat-abi=softfp -O3" \
    --extra-ldflags="" \
    --cc="${CROSS_PREFIX}armv7a-linux-androideabi21-clang" \
    --cxx="${CROSS_PREFIX}armv7a-linux-androideabi21-clang++" \
    --nm="${CROSS_PREFIX}${NDK_ABIARCH}-nm" \
    --ld="${CROSS_PREFIX}armv7a-linux-androideabi21-clang" \
    --ar="${CROSS_PREFIX}${NDK_ABIARCH}-ar" \
    --strip="${CROSS_PREFIX}${NDK_ABIARCH}-strip" \
    --extra-ldexeflags=-pie \
    --disable-static \
    --disable-neon \
    --enable-shared \
    --disable-doc \
    --disable-programs \
    --disable-everything \
    --disable-avdevice \
    --disable-avformat \
    --disable-swscale \
    --disable-postproc \
    --disable-avfilter \
    --disable-symver \
    --enable-avresample \
    --enable-decoder=h264 \
    --enable-decoder=aac_latm \
    --enable-decoder=ac3 \
    --enable-decoder=eac3 \
    --enable-decoder=mp3 || \
    exit 1

make -j4
make install-libs

make clean
find . -name "*.o" | xargs rm

make install-headers
