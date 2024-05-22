import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Math;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class HighestAverageTFIDF {

    public static class FirstMapper
        extends Mapper<Object, Text, Text, Text>{

        private Text termIdAndClassName = new Text();
        private Text docIdAndTFIDF = new Text();
        private static HashMap<Integer, String> docIdToClassName = new HashMap<>();

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            Path[] cacheFiles = context.getLocalCacheFiles();
            String line;

            BufferedReader reader = new BufferedReader(new InputStreamReader(FileSystem.getLocal(conf).open(cacheFiles[0])));
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");    // tech.401	2225
                String className = parts[0].split("\\.")[0];   // tech
                int docId = Integer.parseInt(parts[1]);     // 2225
                docIdToClassName.put(docId, className);
            }
            reader.close();
        }

        public void map(Object key, Text value, Context context
                        ) throws IOException, InterruptedException {

            String[] parts = value.toString().split("\\s+");

            String termId = parts[0];
            int docId = Integer.parseInt(parts[1]);
            String tf_idf = parts[2];
            String className = docIdToClassName.get(docId);

            termIdAndClassName.set(termId + " " + className);
            docIdAndTFIDF.set(docId + " " + tf_idf);
            context.write(termIdAndClassName, docIdAndTFIDF);
        }
    }

    public static class FirstReducer
        extends Reducer<Text, Text, Text, DoubleWritable> {

        public void reduce(Text key, Iterable<Text> values, Context context
                        ) throws IOException, InterruptedException {

            double sumTFIDF = 0;
            int numTFIDF = 0;

            for (Text val : values) {
                String[] parts = val.toString().split(" ");
                String docId = parts[0];
                String tf_idf = parts[1];

                sumTFIDF += Double.parseDouble(tf_idf);
                numTFIDF++;
            }

            DoubleWritable averageTFIDF = new DoubleWritable(sumTFIDF / numTFIDF);
            context.write(key, averageTFIDF);
        }
    }

    public static class SecondMapper
        extends Mapper<Object, Text, Text, Text>{

        private Text classNameText = new Text();
        private Text termIdAndAvgTFIDF = new Text();

        public void map(Object key, Text value, Context context
                        ) throws IOException, InterruptedException {

            String[] parts = value.toString().split("\\s+");

            String termId = parts[0];
            String className = parts[1];
            String avgTFIDF = parts[2];

            classNameText.set(className);
            termIdAndAvgTFIDF.set(termId + " " + avgTFIDF);
            context.write(classNameText, termIdAndAvgTFIDF);
        }
    }

    public static class SecondReducer
        extends Reducer<Text, Text, Text, Text> {

        private static HashMap<Integer, String> termIdToTerm = new HashMap<>();

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            Path[] cacheFiles = context.getLocalCacheFiles();
            String line;

            BufferedReader reader = new BufferedReader(new InputStreamReader(FileSystem.getLocal(conf).open(cacheFiles[0])));
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");    // yeah	6774
                String term = parts[0];   // yeah
                int termId = Integer.parseInt(parts[1]);     // 6774
                termIdToTerm.put(termId, term);
            }
            reader.close();
        }

        public void reduce(Text key, Iterable<Text> values, Context context
                        ) throws IOException, InterruptedException {

            TreeMap<Double, Text> top5 = new TreeMap<>();

            for (Text val : values) {
                String[] parts = val.toString().split(" ");
                int termId = Integer.parseInt(parts[0]);
                double avgTFIDF = Double.parseDouble(parts[1]);
                String term = termIdToTerm.get(termId);

                top5.put(avgTFIDF, new Text(term + ": " + String.format("%.3f", avgTFIDF)));
                if (top5.size() > 5) {
                    top5.remove(top5.firstKey());
                }
            }

            StringBuilder entryValues = new StringBuilder();
            for (Map.Entry<Double, Text> entry : top5.descendingMap().entrySet()) {
                if (entryValues.length() > 0) {
                    entryValues.append(", ");
                }
                entryValues.append(entry.getValue());
            }

            Text newKey = new Text(key.toString() + ": ");
            context.write(newKey, new Text(entryValues.toString()));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf1 = new Configuration();
        Job job1 = Job.getInstance(conf1, "first job");

        job1.setJarByClass(HighestAverageTFIDF.class);
        job1.setMapperClass(FirstMapper.class);
        job1.setReducerClass(FirstReducer.class);

        job1.setMapOutputKeyClass(Text.class);
        job1.setMapOutputValueClass(Text.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(DoubleWritable.class);

        job1.addCacheFile(new Path(args[3]).toUri());   // docid.mtx

        FileInputFormat.addInputPath(job1, new Path(args[0]));  // task_1_4.mtx
        FileOutputFormat.setOutputPath(job1, new Path(args[1]));    // Intermediate

        if (!job1.waitForCompletion(true)) {
            System.exit(1);
        }

        Configuration conf2 = new Configuration();
        Job job2 = Job.getInstance(conf2, "second job");

        job2.setJarByClass(HighestAverageTFIDF.class);
        job2.setMapperClass(SecondMapper.class);
        job2.setReducerClass(SecondReducer.class);

        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);

        job2.addCacheFile(new Path(args[4]).toUri());    // termid.mtx

        FileInputFormat.addInputPath(job2, new Path(args[1]));  // Intermediate
        FileOutputFormat.setOutputPath(job2, new Path(args[2]));    // Output

        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}
