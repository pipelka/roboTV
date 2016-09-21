SUBDIRS = \
	doc \
	include \
	src \
	tests

DIST_SUBDIRS = \
	jni \
	$(SUBDIRS)

pkgconfigdir = $(prefix)/lib/pkgconfig
pkgconfig_DATA = msgexchange.pc

EXTRA_DIST = \
	$(pkgconfig_DATA) \
	jni/Android.mk \
	jni/Application.mk \
	jni/Makefile

$(PACKAGE)-$(VERSION).tar.gz: dist

astyle:
	astyle -R "include/*.h" "src/*.cpp" "tests/*.cpp"
