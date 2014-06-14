
all : comp

comp : comparer.o utils.o
	$(CC) $(CFLAGS) -o comparer $^

comparer.o : utils.h
utils.o : utils.h 


.PHONY : clean
clean :
	-rm -f *.o comparer core
