import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.util.ArrayList;
import java.util.List;
public class TermFrequencyFilter {

    public static class TokenizerMapper
            extends Mapper<LongWritable, Text, LongWritable, Text> {

        private LongWritable outputKey = new LongWritable();
        private Text outputValue = new Text();

        public void map(LongWritable key, Text value, Context context
        ) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            int termid = Integer.parseInt(itr.nextToken());
            String docid = itr.nextToken();
            float frequency = Float.parseFloat(itr.nextToken());

            outputKey.set(termid);
            outputValue.set(docid + "\t" + frequency);
            context.write(outputKey, outputValue);
        }
    }

    public static class FloatSumReducer
        extends Reducer<LongWritable, Text, LongWritable, Text> {
	    
        private Text outputValue = new Text();
	
        public void reduce(LongWritable key, Iterable<Text> values, Context context) 
            throws IOException, InterruptedException {

            float sum = 0;
            List<String> listStr = new ArrayList<String>();
            for (Text val : values) {
                StringTokenizer itr = new StringTokenizer(val.toString(), "\t");
                String docid = itr.nextToken();
                float frequency = Float.parseFloat(itr.nextToken());
                sum += frequency;
                listStr.add(docid + "\t" + frequency);
            }
            if (sum >= 3) {
                for (String val : listStr) {
                    outputValue.set(val);
                    context.write(key, outputValue);
                }
                    
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "TermFrequencyFilter");
        job.setJarByClass(TermFrequencyFilter.class);
        job.setMapperClass(TokenizerMapper.class);
        
        job.setReducerClass(FloatSumReducer.class);
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
