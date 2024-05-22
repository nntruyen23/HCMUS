import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.FileReader;

public class TermDocFreq {

    public static class WordMapper extends Mapper<LongWritable, Text, Text, LongWritable> {

        private final static LongWritable one = new LongWritable(1);
        private String classAndDoc;
        private Set<String> stopWords = new HashSet<>();
        private static HashMap<String, Integer> wordIdMap = new HashMap<>();
        private static HashMap<String, Integer> docIdMap = new HashMap<>(); 

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            String className = ((FileSplit) context.getInputSplit()).getPath().getParent().getName();   // business
            String docName = ((FileSplit) context.getInputSplit()).getPath().getName(); // 001.txt

            String[] docNameParts = docName.split("\\.");   
            String docId = docNameParts[0];   // 001

            classAndDoc = className + "." + docId;   // business.123

            Configuration conf = context.getConfiguration();
            Path[] cacheFiles = context.getLocalCacheFiles();
            String line;

            BufferedReader readerStopWords = new BufferedReader(new InputStreamReader(FileSystem.getLocal(conf).open(cacheFiles[0])));
            while ((line = readerStopWords.readLine()) != null) {
                stopWords.add(line.trim());
            }
            readerStopWords.close();

            BufferedReader readerWordIdMap = new BufferedReader(new InputStreamReader(FileSystem.getLocal(conf).open(cacheFiles[1])));
            while ((line = readerWordIdMap.readLine()) != null) {
                String[] parts = line.split("\\s+");
                wordIdMap.put(parts[0], Integer.parseInt(parts[1]));
            }
            readerWordIdMap.close();

            BufferedReader readerDocIdMap = new BufferedReader(new InputStreamReader(FileSystem.getLocal(conf).open(cacheFiles[2])));
            while ((line = readerDocIdMap.readLine()) != null) {
                String[] parts = line.split("\\s+");
                docIdMap.put(parts[0], Integer.parseInt(parts[1]));
            }
            readerDocIdMap.close();
        }

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {          

            StringTokenizer tokenizer = new StringTokenizer(value.toString(), " \t\n\r\f_");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                String word = token.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(); // 

                if (!stopWords.contains(word) && wordIdMap.containsKey(word)) {
                    int termId = wordIdMap.get(word);
                    int docId = docIdMap.get(classAndDoc);
                    Text keyText = new Text(termId + "\t" + docId); 
                    context.write(keyText, one);
                }
            }
        }
    }

    public static class WordReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context)
                throws IOException, InterruptedException {
            long count = 0;
            for (LongWritable value : values) {
                count += value.get();
            }
            context.write(key, new LongWritable(count));
        }
    }

    public static void main(String[] args) throws Exception {

        Configuration confWC = new Configuration();
        Job jobWC = Job.getInstance(confWC, "word count");

        jobWC.setJarByClass(TermDocFreq.class);
        jobWC.setMapperClass(WordMapper.class);
        jobWC.setCombinerClass(WordReducer.class);
        jobWC.setReducerClass(WordReducer.class);

        jobWC.setOutputKeyClass(Text.class);
        jobWC.setOutputValueClass(LongWritable.class);
        jobWC.setOutputFormatClass(TextOutputFormat.class);

        jobWC.addCacheFile(new Path(args[2]).toUri());  // stopwords.txt
        jobWC.addCacheFile(new Path(args[3]).toUri());  // termid.mtx
        jobWC.addCacheFile(new Path(args[4]).toUri());  // docid.mtx

        TextInputFormat.addInputPath(jobWC, new Path(args[0]));     // bbc
        FileInputFormat.setInputDirRecursive(jobWC, true);
        TextOutputFormat.setOutputPath(jobWC, new Path(args[1]));   // Output

        System.exit(jobWC.waitForCompletion(true) ? 0 : 1);
    }
}
