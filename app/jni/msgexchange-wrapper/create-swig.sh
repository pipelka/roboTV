#!/bin/sh

cd `dirname $0`
PWD=`pwd`

JNI_MSGEXCHANGE__WRAP=${PWD}/msgexchange_wrap.cpp
JNI_MSGEXCHANGE__WRAP_H=${PWD}/msgexchange_wrap.h
JNI_MSGEXCHANGE__I=${PWD}/msgexchange.i
JNI_MSGEXCHANGE__INCLUDE=${PWD}/../msgexchange/include
JNI_MSGEXCHANGE__PACKAGE=org.xvdr.msgexchange
JNI_MSGEXCHANGE__DEPS=${JNI_MSGEXCHANGE__INCLUDE} ${PWD}/msgexchange.i

OUTDIR=${PWD}/../../src/main/java/org/xvdr/msgexchange

mkdir -p ${OUTDIR}
swig -v -c++ -java -package ${JNI_MSGEXCHANGE__PACKAGE} -I${JNI_MSGEXCHANGE__INCLUDE} -outdir ${OUTDIR} -o ${JNI_MSGEXCHANGE__WRAP} ${JNI_MSGEXCHANGE__I}
