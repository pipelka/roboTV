#!/bin/sh

cd `dirname $0`
PWD=`pwd`

JNIWRAP_WRAP=${PWD}/jniwrap.cpp
JNIWRAP_WRAP_H=${PWD}/jniwrap.h
JNIWRAP_I=${PWD}/jniwrap.i
JNIWRAP_INCLUDE=${PWD}/../msgexchange/include
JNIWRAP_PACKAGE=org.robotv.msgexchange
JNIWRAP_DEPS=${JNI_MSGEXCHANGE__INCLUDE} ${PWD}/jniwrap.i

OUTDIR=${PWD}/../../java/org/robotv/msgexchange

mkdir -p ${OUTDIR}
swig -v -c++ -java -package ${JNIWRAP_PACKAGE} -I${JNIWRAP_INCLUDE} -outdir ${OUTDIR} -o ${JNIWRAP_WRAP} ${JNIWRAP_I}
