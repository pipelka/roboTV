#!/bin/sh

cd `dirname $0`/..

# build the roboTV release apk
# keys are taken from the environment variables:

# RELEASE_STORE_PASSWORD
# RELEASE_KEY_ALIAS
# RELEASE_KEY_PASSWORD

# set appropriately before running this this script

./gradlew cleanBuildCache clean bundleRelease

VERSION=`git describe --tags`
AABDIR=distribution/bin
AAB=robotv-${VERSION}.aab
AABFILE=${AABDIR}/${AAB}

mkdir -p ${AABDIR}
cp -f ./robotv-atv/build/outputs/bundle/release/robotv-release.aab ${AABFILE}

echo
echo "Release Bundle:"
echo `pwd`/${AABFILE}
