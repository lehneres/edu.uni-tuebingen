all : k-medians

k-medians : main.o k-medians.o utils.o
	$(CC) $(CFLAGS) -o k-medians-solver $^

main.o : approx.h utils.h
k-medians.o : approx.h utils.h
utils.o : utils.h

.PHONY : clean
clean :
	-rm -f k-medians-solver *.o core
