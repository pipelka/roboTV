SOURCES := $(shell find svg -name "*.svg")
TARGETS = $(SOURCES:svg/%.svg=png/%.png)
CONVERT := convert

all: $(TARGETS)

png/%.png: svg/%.svg
	$(CONVERT) -resize 1024 $< $@

clean: *.png
	rm $<
