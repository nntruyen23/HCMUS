import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TFIDF_Transform {

    public static class TokenizerMapper
            extends Mapper<Object, Text, IntWritable, Text> {

        private IntWritable docId = new IntWritable();
        private Text termWithTFIDF = new Text();

        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            String[] tokens = value.toString().split("\\s+");
            if (tokens.length == 3) {
                int docIdInt = Integer.parseInt(tokens[1]);
                String termId = tokens[0];
                String tfidf = tokens[2];

                docId.set(docIdInt);
                termWithTFIDF.set(termId + ":" + tfidf);
                context.write(docId, termWithTFIDF);
            }
        }
    }

    public static class IntSumReducer
            extends Reducer<IntWritable, Text, IntWritable, Text> {

        private Text result = new Text();

        public void reduce(IntWritable key, Iterable<Text> values,
                           Context context
        ) throws IOException, InterruptedException {
            String str = "";
            for (Text val : values) {
            	str = str + val.toString() + ",";
            }
            if (str.length() > 0) {
   		str = str.substring(0, str.length() - 1);
	    }
            result.set(str);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "TF-IDF Transform");
        job.setJarByClass(TFIDF_Transform.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

