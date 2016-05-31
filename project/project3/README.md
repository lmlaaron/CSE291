Instruction

1. Configure your hadoop.

2. Make to file into .jar.

3. cd $HADOOP\_PREFIX

4. bin/hadoop jar directory/to/jarfile input\_directory output\_directory

5. Check with bin/hdfs dfs -get hdfsfile localfile; cat localfile;

6. Remove with bin/hdfs dfs -rm -r hdfsfile; rm -rf localfile;
