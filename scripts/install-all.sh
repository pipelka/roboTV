#!/bin/sh

cd `dirname $0`/..

# the environment variable ATV_DEVICES should contain a list of all devices

# for example:
# ATV_DEVICES="192.168.1.1:5555 192.168.1.2:5555 192.168.1.3:5555"

for DEVICE in ${ATV_DEVICES}; do
    adb connect ${DEVICE}
done

./gradlew clean assembleDebug installDebug

for DEVICE in ${ATV_DEVICES}; do
    adb disconnect ${DEVICE}
done
