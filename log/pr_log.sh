for i in `seq 1 34`;
do
    grep -i "job #$i" stats.txt | wc -l
done
