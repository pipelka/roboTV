m4_define([MAJOR], 0)
m4_define([MINOR], 6)
m4_define([MICRO], 3)

dnl Process this file with autoconf to produce a configure script.
AC_INIT([libmsgexchange], [MAJOR.MINOR.MICRO], [pipelka@bms-austria.com])

# Making releases:
#   MICRO_VERSION += 1;
#   INTERFACE_AGE += 1;
#   BINARY_AGE += 1;
# if any functions have been added, set INTERFACE_AGE to 0.
# if backwards compatibility has been broken,
# set BINARY_AGE and INTERFACE_AGE to 0.

MSGEXCHANGE_MAJOR_VERSION=MAJOR
MSGEXCHANGE_MINOR_VERSION=MINOR
MSGEXCHANGE_MICRO_VERSION=MICRO
MSGEXCHANGE_INTERFACE_AGE=0
MSGEXCHANGE_BINARY_AGE=3
MSGEXCHANGE_VERSION=$MSGEXCHANGE_MAJOR_VERSION.$MSGEXCHANGE_MINOR_VERSION.$MSGEXCHANGE_MICRO_VERSION

AC_SUBST(MSGEXCHANGE_MAJOR_VERSION)
AC_SUBST(MSGEXCHANGE_MINOR_VERSION)
AC_SUBST(MSGEXCHANGE_MICRO_VERSION)
AC_SUBST(MSGEXCHANGE_INTERFACE_AGE)
AC_SUBST(MSGEXCHANGE_BINARY_AGE)
AC_SUBST(MSGEXCHANGE_VERSION)

GIT_REV=$(git rev-parse --short HEAD 2>/dev/null)
INTEGRATED_REV=$(head -n 1 debian/changelog 2>/dev/null | cut -f2 -d'(' | cut -f1 -d')')
if test "x$INTEGRATED_REV" = "x" ; then
    INTEGRATED_REV="$PACKAGE_VERSION"
fi

AC_DEFINE_UNQUOTED([VERSION_MAJOR], $MSGEXCHANGE_MAJOR_VERSION, [libmsgexchange major version])
AC_DEFINE_UNQUOTED([VERSION_MINOR], $MSGEXCHANGE_MINOR_VERSION, [libmsgexchange minor version])
AC_DEFINE_UNQUOTED([VERSION_MICRO], $MSGEXCHANGE_MICRO_VERSION, [libmsgexchange micro version])
AC_DEFINE_UNQUOTED([GIT_REV], "$GIT_REV", [libmsgexchange git revision])
AC_DEFINE_UNQUOTED([INTEGRATED_REV], "$INTEGRATED_REV", [libmsgexchange integrated revision])

# libtool versioning
LT_RELEASE=$MSGEXCHANGE_MAJOR_VERSION.$MSGEXCHANGE_MINOR_VERSION
LT_CURRENT=`expr $MSGEXCHANGE_MICRO_VERSION - $MSGEXCHANGE_INTERFACE_AGE`
LT_REVISION=$MSGEXCHANGE_INTERFACE_AGE
LT_AGE=`expr $MSGEXCHANGE_BINARY_AGE - $MSGEXCHANGE_INTERFACE_AGE`

AC_SUBST(LT_RELEASE)
AC_SUBST(LT_CURRENT)
AC_SUBST(LT_REVISION)
AC_SUBST(LT_AGE)

AC_CONFIG_AUX_DIR(autotools)

AC_CANONICAL_HOST
AC_CANONICAL_TARGET

dnl Setup for automake
AM_INIT_AUTOMAKE
AM_CONFIG_HEADER(config.h)

dnl Check for tools

AC_LIBTOOL_WIN32_DLL
AM_PROG_LIBTOOL
AC_PROG_MAKE_SET
AC_PROG_CXX
AC_PROG_CPP
AC_PROG_INSTALL
AC_EXEEXT

dnl Check for pkgconfig first
AC_CHECK_PROG(HAVE_PKGCONFIG, pkg-config, yes, no)

dnl Give error and exit if we don't have pkgconfig
if test "x$HAVE_PKGCONFIG" = "xno"; then
  AC_MSG_ERROR(you need to have pkgconfig installed !)
fi

dnl Check for zlib
zlib_found=yes
ZLIB_LIBS=
AC_CHECK_HEADER(zlib.h,,[zlib_found="no"])
if test x$zlib_found = xyes; then
  AC_SEARCH_LIBS(compress, z, [AC_DEFINE([HAVE_ZLIB], 1, [have zlib compression library installed])])
  ZLIB_LIBS="-lz"
  AC_SUBST(ZLIB_LIBS)
fi

dnl Check for pthread
pthread_found=yes
PTHREAD_LIBS=
AC_CHECK_HEADER(pthread.h,,[pthread_found="no"])
if test x$pthread_found = xyes; then
  AC_SEARCH_LIBS(pthread_create, pthread, [AC_DEFINE([HAVE_PTHREAD], 1, [have pthread library installed])])
  PTHREAD_LIBS="-lpthread"
  AC_SUBST(PTHREAD_LIBS)
fi

ws2_found=yes
WS2_LIBS=
AC_CHECK_HEADER(winsock2.h,,[ws2_found="no"])
if test x$ws2_found = xyes; then
  WS2_LIBS="-lws2_32"
  AC_SUBST(WS2_LIBS)
fi

AC_LANG([C++])
AC_CHECK_HEADERS([gtest/gtest.h], [GTEST_LIBS=-lgtest])
AC_SUBST(GTEST_LIBS)

MSGEXCHANGE_CFLAGS="-I$prefix/include/msgexchange"
MSGEXCHANGE_LIBS="-lmsgexchange"

AC_SUBST(MSGEXCHANGE_CFLAGS)
AC_SUBST(MSGEXCHANGE_LIBS)

# check is JSON is enabled
AC_ARG_ENABLE(
  [json], 
  [AS_HELP_STRING([--enable-json],[enable JSON support (default no)])], 
  [case "${enableval}" in
    yes) json=true ;;
    no)  json=false ;;
    *) AC_MSG_ERROR([bad value ${enableval} for --enable-json]) ;;
  esac
  AC_DEFINE_UNQUOTED([ENABLE_JSON], 1, [JSON support])
  ],[json=false])
AM_CONDITIONAL([JSON], [test x$json = xtrue])

case "$target" in
  *-*-cygwin* | *-*-mingw32*)
	MINGW32FLAGS="-no-undefined -no-cygwin"
	AC_SUBST(MINGW32FLAGS)
  ;;
esac

# Finally create all the generated files
AC_CONFIG_FILES([upload.sh], [chmod +x upload.sh])

AC_OUTPUT([
Makefile
doc/Makefile
include/Makefile
jni/Makefile
src/Makefile
tests/Makefile
msgexchange.pc
doc/msgexchange.doxy
])
