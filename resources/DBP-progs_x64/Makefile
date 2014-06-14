CC=gcc
INCLUDES=$(CURDIR)/utils/
CFLAGS=-O3 -ansi -pedantic -Wall -I$(INCLUDES) -D_GNU_SOURCE
VPATH=$(INCLUDES)

export CC CFLAGS INCLUDES VPATH

PROGS = asso k-medians decomp comparer mod-matrix iter

all : progs
.PHONY : progs $(PROGS) clean dist-clean
progs : $(PROGS)
asso : 
	$(MAKE) -C asso-solver/

k-medians : 
	$(MAKE) -C k-medians-solver/

decomp : 
	$(MAKE) -C create-decomp/

comparer: 
	$(MAKE) -C comparer/

mod-matrix:
	$(MAKE) -C mod-matrix/

iter :
	$(MAKE) -C iter-solver/

clean :
	cd asso-solver && $(MAKE) clean
	cd utils && $(MAKE) clean
	cd k-medians-solver && $(MAKE) clean
	cd create-decomp && $(MAKE) clean
	cd comparer && $(MAKE) clean
	cd mod-matrix && $(MAKE) clean
	cd iter-solver && $(MAKE) clean
dist-clean : clean
	find . -iname '*~' | xargs rm -f
