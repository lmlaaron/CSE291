Instruction

1. Configure your hadoop.

2. Make to file into .jar.

3. cd $HADOOP\_PREFIX

4. bin/hadoop jar directory/to/jarfile input\_directory output\_directory

5. Check with bin/hdfs dfs -get hdfsfile localfile; cat localfile;

6. Remove with bin/hdfs dfs -rm -r hdfsfile; rm -rf localfile;

The batch.sh file is to generate the output information required in the handout, though it is mixed with the log. The true outputs are like:

======================
Top 10% bigrams =  36
======================

=====================================
Top 10 bigrams and their population
of the	7
to the	5
as a	4
at the	3
does not	3
donald trump	3
he does	3
a team	2
anything other	2
be the	2
=====================================

Run the batch.sh file with command ./batch.sh input\_file\_in\_hdfs

Please be very sure that the variable $HADOOP\_PREFIX is set to the directory of Hadoop installation.
