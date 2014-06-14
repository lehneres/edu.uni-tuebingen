#!/bin/bash
echo $1 $2 $3 $4 $5

sed '/^$/d' -i $1



ATTRIBUTES=$( echo 0$( grep @attribute $1 | wc -l) + 0$(grep @ATTRIBUTE  $1 | wc -l) | bc )
DATALINE=$( echo 0$( grep -n @data $1 | sed -e 's/:.*//g' ) + 0$( grep -n @DATA $1 | sed -e 's/:.*//g' ) | bc)
INSTANCES=$( echo "$(  wc -l $1 | sed -e 's/ .*//g') - $DATALINE" | bc  ) 

echo $INSTANCES > tmpfile$1
echo $ATTRIBUTES >> tmpfile$1
tail -n $INSTANCES $1 >> tmpfile$1 
sed -e 's/,/ /g' -i tmpfile$1
sed -e 's/?/0/g' -i tmpfile$1

sed '/^$/d' -i tmpfile$1


resources/DBP-progs_x64/iter-solver/iter-solver -stmpfile$1 -DfinaltmpD$1 -t$3 -k$4 -bfinaltmpb$1


LINES=$( echo $( wc -l finaltmpD$1 | sed -e 's/ .*//g' ) - 2 | bc )
echo "@relation l" > $2
for i in `seq 1 $4` ; do echo "@attribute decomposition_dc_$i {0,1}" >> $2; done
echo "@data" >> $2
tail -n $LINES finaltmpD$1 | sed -e 's/\(.*\) /\1/g' | sed -e 's/ /,/g'  >>  $2

HEADERTMP=""

LINES=$( echo $( wc -l finaltmpb$1 | sed -e 's/ .*//g' ) - 2 | bc )
echo "@relation u" > $5
for i in `seq 1 $ATTRIBUTES` ; do echo "@attribute decomposition_bc_$i {0,1}" >> $5; done
echo "@data" >> $5
tail -n $LINES finaltmpb$1 | sed -e 's/\(.*\) /\1/g' | sed -e 's/ /,/g'  >>  $5

rm finaltmpD$1 finaltmpb$1 tmpfile$1
