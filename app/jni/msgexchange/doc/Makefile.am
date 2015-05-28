OUTFILE = classdiagram.uml
UMLHEADERS = $(srcdir)/../include/*.h

all: $(OUTFILE)

$(OUTFILE): $(UMLHEADERS)
	echo "@startuml" > $(OUTFILE)
	echo "package \"libmsgexchange\" #DDDDDD {" >> $(OUTFILE)
	for file in $(UMLHEADERS) ; do \
		sed -n '/@startuml/,/@enduml/p' $$file | grep -v "@startuml" | grep -v "@enduml" >> $(OUTFILE) ; \
	done
	echo "}" >> $(OUTFILE)
	echo "@enduml" >> $(OUTFILE)

doc: msgexchange.doxy
	rm -Rf html
	doxygen msgexchange.doxy

upload: doc
	scp -r html/* root@packages.bms-austria.com:/srv/packages/deathray/documentation/msgexchange

EXTRA_DIST = \
	README \
	$(OUTFILE) \
	sequencediagram.uml
