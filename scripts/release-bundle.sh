#!/bin/sh

cd `dirname $0`/..

# build the roboTV release apk
# keys are taken from the environment variables:

# RELEASE_STORE_PASSWORD
# RELEASE_KEY_ALIAS
# RELEASE_KEY_PASSWORD

# set appropriately before running this this script

./gradlew cleanBuildCache clean bundleRelease

VERSION=`git describe`
AAB=robotv-${VERSION}.aab

mkdir -p bin
cp -f ./robotv-atv/build/outputs/bundle/release/robotv-release.aab ${AAB}

echo
echo "Release Bundle:"
echo `pwd`/bin/${AAB}
