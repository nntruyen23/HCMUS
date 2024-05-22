import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.lang.Math;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TFIDF {

    private static long numDocs = 0; // Shared field

    public static class FirstMapper
        extends Mapper<Object, Text, Text, Text>{

        private Text documentId = new Text();
        private Text termIdAndFrequency = new Text();

        public void map(Object key, Text value, Context context
                        ) throws IOException, InterruptedException {
            String[] parts = value.toString().split("\\s+");
            String termId = parts[0];
            String docId = parts[1];
            String freq = parts[2];

            documentId.set(docId);
            termIdAndFrequency.set(termId + " " + freq);
            context.write(documentId, termIdAndFrequency);
        }
    }

    public static class FirstReducer
        extends Reducer<Text, Text, Text, DoubleWritable> {

        private Text new_key = new Text();
        private DoubleWritable result = new DoubleWritable();
        private Set<String> uniqueDocIDs = new HashSet<>();

        public void reduce(Text key, Iterable<Text> values, Context context
                        ) throws IOException, InterruptedException {

            uniqueDocIDs.add(key.toString());

            double totalWords = 0;
            List<Text> valueList = new ArrayList<>();

            for (Text val : values) {
                String[] parts = val.toString().split(" ");
                double freq = Double.parseDouble(parts[1]);
                totalWords += freq;
                valueList.add(new Text(val));
            }

            for (Text val : valueList) {
                String[] parts = val.toString().split(" ");
                String termId = parts[0];
                double freq = Double.parseDouble(parts[1]);

                double tf = freq / totalWords;
                
                new_key.set(termId + "@" + key);
                result.set(tf);
                context.write(new_key, result);
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            numDocs = uniqueDocIDs.size();
            context.getCounter("CustomCounters", "NUM_DOCS").setValue(numDocs);
        }
    }

    public static class SecondMapper
        extends Mapper<Object, Text, Text, Text>{

        public void map(Object key, Text value, Context context
                        ) throws IOException, InterruptedException {
            String[] parts = value.toString().split("\\s+");
            String termIdAndDocId = parts[0];
            String tf = parts[1];

            String[] termIdAndDocIdParts = termIdAndDocId.split("@");
            String termId = termIdAndDocIdParts[0];
            String docId = termIdAndDocIdParts[1];

            Text termIdText = new Text(termId);
            Text docIdAndTF = new Text(docId + " " + tf);
            
            context.write(termIdText, docIdAndTF);
        }
    }

    public static class SecondReducer
        extends Reducer<Text, Text, Text, DoubleWritable> {

        private Text new_key = new Text();

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            numDocs = context.getConfiguration().getInt("numDocs", 0); 
        }

        public void reduce(Text key, Iterable<Text> values, Context context
                        ) throws IOException, InterruptedException {

            int numDocOccurrences = 0;
            List<String> valueList = new ArrayList<>();

            for (Text val : values) {
                numDocOccurrences++;
                valueList.add(val.toString());
            }

            for (String val : valueList) {
                String[] parts = val.split(" ");
                String docId = parts[0];
                double tf = Double.parseDouble(parts[1]);
                double idf = Math.log((double) numDocs / numDocOccurrences);
                DoubleWritable tf_idf = new DoubleWritable(tf * idf);

                new_key.set(key + " " + docId);
                context.write(new_key, tf_idf);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf1 = new Configuration();
        Job job1 = Job.getInstance(conf1, "first job");
        job1.setJarByClass(TFIDF.class);
        job1.setMapperClass(FirstMapper.class);
        job1.setReducerClass(FirstReducer.class);

        job1.setMapOutputKeyClass(Text.class);
        job1.setMapOutputValueClass(Text.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.addInputPath(job1, new Path(args[0]));
        FileOutputFormat.setOutputPath(job1, new Path(args[1]));

        if (!job1.waitForCompletion(true)) {
            System.exit(1);
        }

        long numDocs = job1.getCounters().findCounter("CustomCounters", "NUM_DOCS").getValue();

        Configuration conf2 = new Configuration();
        conf2.setLong("numDocs", numDocs);
        Job job2 = Job.getInstance(conf2, "second job");
        job2.setJarByClass(TFIDF.class);
        job2.setMapperClass(SecondMapper.class);
        job2.setReducerClass(SecondReducer.class);

        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.addInputPath(job2, new Path(args[1]));
        FileOutputFormat.setOutputPath(job2, new Path(args[2]));

        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}
