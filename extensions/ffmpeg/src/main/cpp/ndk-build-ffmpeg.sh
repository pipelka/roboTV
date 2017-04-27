#!/bin/sh

cd `dirname $0`

NDK_BUILD=`which ndk-build`
NDK_PATH=`dirname $NDK_BUILD`

# remove dirs

rm -Rf libs
rm -Rf include

# checkout ffmpeg

[ -d ffmpeg ] || ( git clone git://source.ffmpeg.org/ffmpeg ffmpeg && cd ffmpeg && git checkout b96a6e2 && cd .. )

# armeabi-v7a

cd ffmpeg
./configure \
    --prefix=.. \
    --libdir=../libs/armeabi-v7a \
    --arch=arm \
    --cpu=armv7-a \
    --cross-prefix="${NDK_PATH}/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin/arm-linux-androideabi-" \
    --target-os=android \
    --sysroot="${NDK_PATH}/platforms/android-22/arch-arm/" \
    --extra-cflags="-march=armv7-a -mfloat-abi=softfp" \
    --extra-ldflags="-Wl,--fix-cortex-a8" \
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
    --enable-decoder=mp3

make clean
find . -name "*.o" | xargs rm

make -j4
make install-libs


# x86

./configure \
    --prefix=.. \
    --libdir=../libs/x86 \
    --arch=x86 \
    --cpu=i686 \
    --cross-prefix="${NDK_PATH}/toolchains/x86-4.9/prebuilt/linux-x86_64/bin/i686-linux-android-" \
    --target-os=android \
    --sysroot="${NDK_PATH}/platforms/android-22/arch-x86/" \
    --extra-cflags="-std=c99 -O3 -Wall -fpic -pipe -DANDROID -DNDEBUG -march=atom -msse3 -ffast-math -mfpmath=sse" \
    --extra-ldflags="-lm -lz -Wl,--no-undefined -Wl,-z,noexecstack" \
    --enable-version3 \
    --enable-pic \
    --disable-asm \
    --disable-static \
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
    --enable-decoder=mp3

make clean
find . -name "*.o" | xargs rm

make -j4
make install-libs
make install-headers
