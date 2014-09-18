for i in `seq 1 1500`;
do
    grep -i "job #$i" stats.txt | wc -l
done
