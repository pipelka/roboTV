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
APKDIR=distribution/bin
APK=robotv-${VERSION}.apk
APKFILE=${APKDIR}/${APK}

mkdir -p ${APKDIR}
cp -f ./robotv-atv/build/outputs/apk/release/robotv-release.apk ${APKFILE}

echo
echo "Release APK:"
echo `pwd`/${APKFILE}
