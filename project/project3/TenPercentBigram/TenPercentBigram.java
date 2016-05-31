// from https://hadoop.apache.org/docs/current/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TenPercentBigram {
  public static class TokenizerMapper
       extends Mapper<Object, Text, Text, DoubleWritable>{

    private String tokens = "[_|$#<>\\^=\\{\\}\\*/\\\\,;,.\\-:()?!\"']";
    private Map<String, Integer> countMap = new HashMap<>();
    private int total;

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
      int total = 0;
      for (String key: countMap.keySet()) {
        total += countMap.get(key);
      }
      for (String key: countMap.keySet()) {
        context.write(new Text(key), new DoubleWritable(countMap.get(key) * 1.0 / total));
      }
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,DoubleWritable,Text,DoubleWritable> {
    private Map<Text, DoubleWritable> countMap = new HashMap<>();
    
    public static Map sortByValues(Map unsortedMap) {
		Map sortedMap = new TreeMap(new ValueComparator(unsortedMap));
		sortedMap.putAll(unsortedMap);
		return sortedMap;
    }

    @Override
    public void reduce(Text key, Iterable<DoubleWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      double sum = 0;
      for (DoubleWritable val : values) {
        sum += val.get();
      }
      countMap.put(new Text(key), new DoubleWritable(sum));
    }

    protected void cleanup(Context context) throws IOException, InterruptedException {
      Map<Text, DoubleWritable> sortedMap = sortByValues(countMap);
      double countBigrams = 0;
      for (Text k: sortedMap.keySet()) {
        countBigrams += sortedMap.get(k).get();
        context.write(k, new DoubleWritable(sortedMap.get(k).get()));
        if (countBigrams >= 0.10 ) {
          break;
        }
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
    Job job = Job.getInstance(conf, "ten percent bigram");
    job.setJarByClass(TenPercentBigram.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(DoubleWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
