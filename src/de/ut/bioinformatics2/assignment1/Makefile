all: matrix.h sequence.o scorematrix.o nussinov.o rnastructureprediction

rnastructureprediction: rnastructureprediction.cpp matrix.h sequence.o scorematrix.o nussinov.o
	g++ matrix.h sequence.o scorematrix.o nussinov.o -o rnastructureprediction -I. rnastructureprediction.cpp

nussinov.o: nussinov.cpp nussinov.h matrix.h scorematrix.o sequence.o
	g++ -c -O2 -I. nussinov.cpp
	
scorematrix.o: scorematrix.cpp scorematrix.h matrix.h
	g++ -c -O2 -I. scorematrix.cpp

sequence.o: sequence.h sequence.cpp matrix.h
	g++ -c -O2 -I. sequence.cpp

clean :
	rm rnastructureprediction sequence.o scorematrix.o nussinov.o 
