all : mod-matrix.o utils.o
	$(CC) $(CFLAGS) -o mod-matrix $^
mod-matrix.o : utils.h
utils.o : utils.h

.PHONY : clean
clean :
	-rm -f mod-matrix *.o core
