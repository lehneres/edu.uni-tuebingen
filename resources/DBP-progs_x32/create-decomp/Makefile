
all : create-decomp

create-decomp : create-decomp.o utils.o
	$(CC) $(CFLAGS) -lm -o create-decomp $^

create-decomp.o : utils.h
utils.o : utils.h
#	$(MAKE) -C $(INCLUDES)

.PHONY : clean
clean :
	-rm -f *.o create-decomp core 
