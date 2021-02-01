#!/bin/sh

cd `dirname $0`/..

# build the roboTV release apk
# keys are taken from the environment variables:

# RELEASE_STORE_PASSWORD
# RELEASE_KEY_ALIAS
# RELEASE_KEY_PASSWORD

# set appropriately before running this this script

./gradlew cleanBuildCache clean bundleDebug

VERSION=`git describe`
AAB=robotv-debug-${VERSION}.aab

mkdir -p bin
cp -f ./robotv-atv/build/outputs/bundle/debug/robotv-debug.aab ./bin/${AAB}

echo
echo "Debug Bundle:"
echo `pwd`/bin/${AAB}
