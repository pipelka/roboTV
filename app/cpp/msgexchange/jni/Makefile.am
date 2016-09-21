PKGDIR = com/bmsitech/libs/msgexchange

all: .built 

obj/$(PKGDIR):
	mkdir -p obj/$(PKGDIR)

.built:
	ndk-build

clean:
	$(RM) msgexchange_wrap.cpp
	$(RM) -R obj
	ndk-build clean

.PHONY: .built
