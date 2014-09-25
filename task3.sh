#---------------------------------------------------
# use for as in C-programming
# sum of the first n integer numbers
#---------------------------------------------------
#!/bin/bash

s=0 # here sum

for((i=1; i <= 20000 ; i++))
do
    let s=$s+$i
done
