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
line=$(wc -l < bigram/part-*);
echo "====================================="
echo "Total number of distinct bigrams = " $line
echo "====================================="
make clean;
rm -rf bigram;
cd ..;

cd TopNBigramCount;
make;
$HADOOP_PREFIX/bin/hdfs dfs -rm -r topn;
rm -rf topn;
$HADOOP_PREFIX/bin/hadoop jar topnbigramcount.jar $1 topn;
$HADOOP_PREFIX/bin/hdfs dfs -get topn topn
echo "====================================="
echo "The most popular bigram = " $(head -n 1 topn/part-* | awk '{print $1 " " $2;}')
echo "====================================="

echo "====================================="
echo "Top 10 bigrams and their population"
cat topn/part-*;
echo "====================================="
make clean;
rm -rf topn;
cd ..;

cd TenPercentBigram;
make;
$HADOOP_PREFIX/bin/hdfs dfs -rm -r ten;
rm -rf ten;
$HADOOP_PREFIX/bin/hadoop jar tenpercenbigram.jar $1 ten;
$HADOOP_PREFIX/bin/hdfs dfs -get ten ten
line=$(wc -l < ten/part-*);
echo "======================"
echo "Top 10% bigrams = " $line
echo "======================"
make clean;
rm -rf ten;
cd ..;
