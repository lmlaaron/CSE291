// from https://hadoop.apache.org/docs/current/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TopNBigramCount {
  public static class TokenizerMapper
       extends Mapper<Object, Text, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private String tokens = "[_|$#<>\\^=\\{\\}\\*/\\\\,;,.\\-:()?!\"']";
    private Map<String, Integer> countMap = new HashMap<>();

    @Override
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      String cleanLine = value.toString().toLowerCase().replaceAll(tokens, " ");
      StringTokenizer itr = new StringTokenizer(cleanLine);
      String prev = new String();
      if (itr.hasMoreTokens()) {
        prev = itr.nextToken().trim();
      }
      while (itr.hasMoreTokens()) {
        String curr = itr.nextToken().trim();
        String word = prev + " " + curr;
        if (countMap.containsKey(word)) {
          countMap.put(word, countMap.get(word)+1);
        } else {
          countMap.put(word, 1);
        }
        prev = curr;
      }
    }
    
    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
      for (String key: countMap.keySet()) {
        context.write(new Text(key), new IntWritable(countMap.get(key)));
      }
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private Map<Text, IntWritable> countMap = new HashMap<>();
    
    public static Map sortByValues(Map unsortedMap) {
		Map sortedMap = new TreeMap(new ValueComparator(unsortedMap));
		sortedMap.putAll(unsortedMap);
		return sortedMap;
    }

    @Override
    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      countMap.put(new Text(key), new IntWritable(sum));
    }

    protected void cleanup(Context context) throws IOException, InterruptedException {
      Map<Text, IntWritable> sortedMap = sortByValues(countMap);
      int counter = 0;
      for (Text key: sortedMap.keySet()) {
        counter++;
        if (counter == 10) {
          break;
        }
        context.write(key, sortedMap.get(key));
      }
    }
  }

  public static class ValueComparator implements Comparator {
    Map map;
 
    public ValueComparator(Map map) {
      this.map = map;
    }

    public int compare(Object keyA, Object keyB) {
      Comparable valueA = (Comparable) map.get(keyA);
      Comparable valueB = (Comparable) map.get(keyB);
      int res = valueB.compareTo(valueA);
      if (res == 0) {
        Comparable A = (Comparable) keyA;
        Comparable B = (Comparable) keyB;
        res = A.compareTo(B);
      }
      return res;
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "top n bigram count");
    job.setJarByClass(TopNBigramCount.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
