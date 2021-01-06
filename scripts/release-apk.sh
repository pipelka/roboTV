#!/bin/sh

# build the roboTV release apk
# keys are taken from the environment variables:

# RELEASE_STORE_PASSWORD
# RELEASE_KEY_ALIAS
# RELEASE_KEY_PASSWORD

# set appropriately before running this this script

cd `dirname $0`/..

./gradlew cleanBuildCache clean assembleRelease

VERSION=`git describe`
APK=robotv-${VERSION}.apk

mkdir -p bin
cp -f ./robotv-atv/build/outputs/apk/release/robotv-release.apk ${APK}

echo
echo "Release APK:"
echo `pwd`/bin/${APK}
