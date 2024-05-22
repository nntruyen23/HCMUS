import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.TreeMap;

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

public class MostFrequencyFilter {

    public static class TokenizerMapper
            extends Mapper<LongWritable, Text, Text, Text> {

        private Text outputKey = new Text();
        private Text outputValue = new Text();

        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String[] token = value.toString().split("\\s+");
            String docId = token[0];
            String freq = token[1];
            
            outputKey.set(new Text(Long.toString(key.get())));
            outputValue.set(freq);
            context.write(outputKey, outputValue);
        }
    }

    public static class FloatSumReducer
            extends Reducer<Text, Text, Text, FloatWritable> {
	    
        private FloatWritable outputValue = new FloatWritable();
	
        public void reduce(Text key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            
            float totalFreq = 0f;
            for (Text val : values) {
                float freq = Float.parseFloat(val.toString());
                totalFreq += freq;
            }
            
            outputValue.set(totalFreq);
            context.write(key, outputValue);
        }
    }

    public static class AssignMapper
            extends Mapper<LongWritable, Text, Text, Text> {

        private Text outputKey = new Text("header");
        private Text outputValue = new Text();
        
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {

            Text termFreq = new Text(Long.toString(key.get()) + " " + value.toString());

            outputValue.set(termFreq);
            context.write(outputKey, outputValue);
        }
    }

    public static class SortingReducer
            extends Reducer<Text, Text, Text, Text> {

        private Text outputKey = new Text();
        private Text outputValue = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            TreeMap<Float, String> topValues = new TreeMap<>();
            
            for (Text val : values) {
                String[] token = val.toString().split("\\s+");

                String term = token[0];
                float freq = Float.parseFloat(token[1]);

                topValues.put(freq, term);

                if (topValues.size() > 10) {
                    topValues.pollFirstEntry();
                }
            }
            
            for (Map.Entry<Float, String> entry : topValues.entrySet()) {
                Text term = new Text(entry.getValue());
                Text freq = new Text(Float.toString(entry.getKey()));

                outputKey.set(term);
                outputValue.set(freq);
                
                context.write(outputKey, outputValue);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration firstConf = new Configuration();
        Job freqCountJob = Job.getInstance(firstConf, "FreqCountJob");
        freqCountJob.setJarByClass(MostFrequencyFilter.class);
        
        freqCountJob.setMapperClass(TokenizerMapper.class);
        freqCountJob.setReducerClass(FloatSumReducer.class);
        
        freqCountJob.setOutputKeyClass(Text.class);
        freqCountJob.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(freqCountJob, new Path(args[0]));
        FileOutputFormat.setOutputPath(freqCountJob, new Path(args[1]));
        

        if (!freqCountJob.waitForCompletion(true)) {
            System.exit(1);
        }

        
        Configuration secondConf = new Configuration();
        Job sortingJob = Job.getInstance(secondConf, "SortingJob");
        sortingJob.setJarByClass(MostFrequencyFilter.class);

        sortingJob.setMapperClass(AssignMapper.class);
        sortingJob.setReducerClass(SortingReducer.class);
        
        sortingJob.setOutputKeyClass(Text.class);
        sortingJob.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(sortingJob, new Path(args[1]));  
        FileOutputFormat.setOutputPath(sortingJob, new Path(args[2]));

        System.exit(sortingJob.waitForCompletion(true) ? 0 : 1);
    }
}
