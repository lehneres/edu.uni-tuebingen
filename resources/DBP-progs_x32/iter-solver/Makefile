#CFLAGS=-ansi -pedantic -O3
#CC=gcc

all : iter_full iter_pp

iter_full : main.o iter_full.o iter.o utils.o
	$(CC) $(CFLAGS) -o iter-solver $^

iter_pp : iter_pp.o iter.o utils.o
	$(CC) $(CFLAGS) -o iter-postproc $^

main.o : approx.h utils.h
iter.o : approx.h utils.h iter.h
iter_full.o : approx.h utils.h iter.h
iter_pp : approx.h utils.h iter.h
utils.o : utils.h
#	$(MAKE) -C $(INCLUDES)

.PHONY : clean
clean : 
	-rm -f iter-solver iter-postproc *.o core
