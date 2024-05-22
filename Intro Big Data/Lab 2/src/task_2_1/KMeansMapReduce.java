import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class KMeansMapReduce {

    static final int K = 3;
    static final int MAX_ITERATIONS = 20;

    public static class KMeansMapper extends Mapper<Object, Text, Text, Text> {
        private List<double[]> centroids = new ArrayList<>();

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            FileSystem fs = FileSystem.get(conf);
            String path = conf.get("path");
            Path centroidPath = new Path(path + "/part-r-00000");

            if (!fs.exists(centroidPath)) {
                initializeRandomCentroids();
            } else {
                readCentroidsFromFile(fs, centroidPath);
                ensureEnoughCentroids();
            }
        }

        private void initializeRandomCentroids() {
            Random rand = new Random();
            for (int i = 0; i < K; i++) {
                double[] centroid = new double[2];
                centroid[0] = rand.nextDouble();
                centroid[1] = rand.nextDouble();
                centroids.add(centroid);
            }
        }

        private void readCentroidsFromFile(FileSystem fs, Path centroidPath) throws IOException {
            try (FSDataInputStream inputStream = fs.open(centroidPath);
                 BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    double[] centroid = new double[2];
                    centroid[0] = Double.parseDouble(parts[1]);
                    centroid[1] = Double.parseDouble(parts[2]);
                    centroids.add(centroid);
                }
            }
        }

        private void ensureEnoughCentroids() {
            int centroidCount = centroids.size();
            if (centroidCount < K) {
                Random rand = new Random();
                for (int i = centroidCount; i < K; i++) {
                    double[] centroid = centroids.get(i % centroidCount);
                    centroids.add(new double[]{centroid[0], centroid[1]});
                }
            }
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            if (value.toString().startsWith("class")) {
                return;
            }

            String[] parts = value.toString().split(",");
            double x1 = Double.parseDouble(parts[1]);
            double x2 = Double.parseDouble(parts[2]);

            double minDistance = Double.MAX_VALUE;
            int closestCentroidIndex = -1;

            for (int i = 0; i < K; i++) {
                double[] centroid = centroids.get(i);
                double distance = Math.sqrt(Math.pow(x1 - centroid[0], 2) + Math.pow(x2 - centroid[1], 2));
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCentroidIndex = i;
                }
            }

            context.write(new Text(String.valueOf(closestCentroidIndex)), value);
        }
    }

    public static class KMeansReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            double[] sumX1 = new double[K];
            double[] sumX2 = new double[K];
            int[] count = new int[K];

            for (Text value : values) {
                String[] parts = value.toString().split(",");
                double x1 = Double.parseDouble(parts[1]);
                double x2 = Double.parseDouble(parts[2]);
                int centroidIndex = Integer.parseInt(key.toString());
                sumX1[centroidIndex] += x1;
                sumX2[centroidIndex] += x2;
                count[centroidIndex]++;
            }

            for (int i = 0; i < K; i++) {
                if (count[i] > 0) {
                    double newCentroidX1 = sumX1[i] / count[i];
                    double newCentroidX2 = sumX2[i] / count[i];
                    context.write(key, new Text("," + newCentroidX1 + "," + newCentroidX2));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Path inputPath = new Path(args[0]);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Job job = Job.getInstance(conf, "KMeans MapReduce");
            String centroidPath = args[1] + "/iteration_" + (i + 1);
            Path outputPath = new Path(centroidPath);
            FileOutputFormat.setOutputPath(job, outputPath);
            conf.set("path", centroidPath);

            job.setJarByClass(KMeansMapReduce.class);
            job.setMapperClass(KMeansMapper.class);
            job.setReducerClass(KMeansReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);
            FileInputFormat.addInputPath(job, inputPath);

            if (!job.waitForCompletion(true)) {
                System.exit(1);
            }
    	}
        System.exit(0);
    }
}


