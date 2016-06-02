#! /bin/bash

if [ $# -ne 1 ]
  then
    echo "Exact 1 argument required (input file in HDFS)"
    exit
fi

cd BigramCount;
make;
$HADOOP_PREFIX/bin/hdfs dfs -rm -r bigram;
rm -rf bigram;
$HADOOP_PREFIX/bin/hadoop jar bigramcount.jar $1 bigram;
$HADOOP_PREFIX/bin/hdfs dfs -get bigram bigram
line1=$(wc -l < bigram/part-*);
make clean;
#rm -rf bigram;
cd ..;

cd TopNBigramCount;
make;
$HADOOP_PREFIX/bin/hdfs dfs -rm -r topn;
rm -rf topn;
$HADOOP_PREFIX/bin/hadoop jar topnbigramcount.jar $1 topn;
$HADOOP_PREFIX/bin/hdfs dfs -get topn topn
make clean;
#rm -rf topn;
cd ..;

cd TenPercentBigram;
make;
$HADOOP_PREFIX/bin/hdfs dfs -rm -r ten;
rm -rf ten;
$HADOOP_PREFIX/bin/hadoop jar tenpercenbigram.jar $1 ten;
$HADOOP_PREFIX/bin/hdfs dfs -get ten ten
line3=$(wc -l < ten/part-*);
make clean;
#rm -rf ten;
cd ..;

echo "====================================="
echo "Total number of distinct bigrams = " $line1
echo "====================================="
printf "\n"
echo "====================================="
echo "The most popular bigram = " $(head -n 1 TopNBigramCount/topn/part-* | awk '{print $1 " " $2;}')
echo "====================================="
printf "\n"
echo "====================================="
echo "Top 10 bigrams and their population"
cat TopNBigramCount/topn/part-*;
echo "====================================="
printf "\n"
echo "======================"
echo "Top 10% bigrams = " $line3
echo "======================"
printf "\n"
cd BigramCount;
rm -rf bigram;
cd ..;
cd TopNBigramCount;
rm -rf topn;
cd ..;
cd TenPercentBigram;
rm -rf ten;
cd ..;
