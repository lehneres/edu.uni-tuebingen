#CFLAGS=-ansi -pedantic -O3
#CC=gcc

all : asso

#asso : main.o asso.o utils.o
#	$(CC) $(CFLAGS) -o asso-solver main.o asso.o $(INCLUDES)utils.o
asso : main.o asso.o utils.o
	$(CC) $(CFLAGS) -o asso-solver $^

main.o : approx.h utils.h
asso.o : approx.h utils.h
utils.o : utils.h
#	$(MAKE) -C $(INCLUDES)

.PHONY : clean
clean : 
	-rm -f asso-solver *.o core
