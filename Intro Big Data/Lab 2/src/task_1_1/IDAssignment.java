import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

public class IDAssignment {

    public static class WordIDMapper extends Mapper<Object, Text, Text, IntWritable> {

        private static int lineCount = 1;

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String word = value.toString().trim();
            context.write(new Text(word), new IntWritable(lineCount++));
        }
    }

    public static class DocIDMapper extends Mapper<Object, Text, Text, IntWritable> {

        private static int lineCount = 1;

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String classAndDocId = value.toString().trim();
            context.write(new Text(classAndDocId), new IntWritable(lineCount++));
        }
    }

    public static class WordIDReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            for (IntWritable value : values) {
                context.write(key, value);
            }
        }
    }

    public static class DocIDReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) 
                throws IOException, InterruptedException {
            for (IntWritable value : values) {
                context.write(key, value);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration confWord = new Configuration();
        Job jobWord = Job.getInstance(confWord, "word id assignment");

        jobWord.setJarByClass(IDAssignment.class);
        jobWord.setMapperClass(WordIDMapper.class);
        jobWord.setReducerClass(WordIDReducer.class);

        jobWord.setOutputKeyClass(Text.class);
        jobWord.setOutputValueClass(IntWritable.class);
        jobWord.setInputFormatClass(TextInputFormat.class);
        jobWord.setOutputFormatClass(TextOutputFormat.class);

        TextInputFormat.addInputPath(jobWord, new Path(args[0]));
        TextOutputFormat.setOutputPath(jobWord, new Path(args[1]));

        Configuration confDoc = new Configuration();
        Job jobDoc = Job.getInstance(confDoc, "doc id assignment");

        jobDoc.setJarByClass(IDAssignment.class);
        jobDoc.setMapperClass(DocIDMapper.class);
        jobDoc.setReducerClass(DocIDReducer.class);

        jobDoc.setOutputKeyClass(Text.class);
        jobDoc.setOutputValueClass(IntWritable.class);
        jobDoc.setInputFormatClass(TextInputFormat.class);
        jobDoc.setOutputFormatClass(TextOutputFormat.class);

        TextInputFormat.addInputPath(jobDoc, new Path(args[2]));
        TextOutputFormat.setOutputPath(jobDoc, new Path(args[3]));

        System.exit((jobWord.waitForCompletion(true) && jobDoc.waitForCompletion(true)) ? 0 : 1);
    }
}
