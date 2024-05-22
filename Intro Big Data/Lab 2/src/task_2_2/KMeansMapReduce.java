import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

public class KMeansMapReduce {

    static final int K = 5;
    static final int MAX_ITERATIONS = 10;
    static final int VECTOR_SIZE = 10000;

    public static class KMeansMapper extends Mapper<Object, Text, Text, Text> {
        private List<double[]> centroids = new ArrayList<>();

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            FileSystem fs = FileSystem.get(conf);
            Path centroidPath = new Path("task_2_2.clusters");

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
                double[] centroid = new double[VECTOR_SIZE];
                for (int j = 0; j < VECTOR_SIZE; j++) {
                    centroid[j] = rand.nextDouble();
                }
                centroids.add(centroid);
            }
        }

        private void readCentroidsFromFile(FileSystem fs, Path centroidPath) throws IOException {
            try (BufferedReader br = new BufferedReader(new FileReader(centroidPath.toString()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    double[] centroid = new double[VECTOR_SIZE];
                    for (int i = 0; i < parts.length; i++) {
                        centroid[i] = Double.parseDouble(parts[i]);
                    }
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
                    double[] centroid_copy = new double[VECTOR_SIZE];
                    for (int j = 0; j < VECTOR_SIZE; j++) {
                        centroid_copy[j] = centroid[j];
                    }
                    centroids.add(centroid_copy);
                }
            }
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            if (value.toString().startsWith("class")) {
                return;
            }
            double[] vectorTf_Idf_Double = new double[VECTOR_SIZE];
            Arrays.fill(vectorTf_Idf_Double, 0.0);

            String[] parts = value.toString().split("\\s+");
            int docIdInt = Integer.parseInt(parts[0]);
            String vectorTf_Idf = parts[1];
            String[] vector_string = parts[1].split(",");

            for (String termStr : vector_string) {
                String[] term = termStr.split(":");
                int term_id = Integer.parseInt(term[0]);
                double tf_idf = Double.parseDouble(term[1]);
                vectorTf_Idf_Double[term_id] = tf_idf;
            }
            double minDistance = Double.MAX_VALUE;
            int closestCentroidIndex = -1;

            for (int i = 0; i < K; i++) {
                double[] centroid = centroids.get(i);
                double distance = cosineSimilarity(vectorTf_Idf_Double, centroid);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCentroidIndex = i;
                }
            }

            context.write(new Text(String.valueOf(closestCentroidIndex)), value);
        }

        private double cosineSimilarity(double[] vector1, double[] vector2) {
            double dotProduct = 0.0;
            double magnitude1 = 0.0;
            double magnitude2 = 0.0;

            for (int i = 0; i < vector1.length; i++) {
                dotProduct += vector1[i] * vector2[i];
                magnitude1 += Math.pow(vector1[i], 2);
                magnitude2 += Math.pow(vector2[i], 2);
            }

            magnitude1 = Math.sqrt(magnitude1);
            magnitude2 = Math.sqrt(magnitude2);

            if (magnitude1 == 0 || magnitude2 == 0) {
                return 0.0; // Handle zero vectors
            } else {
                return dotProduct / (magnitude1 * magnitude2);
            }
        }
    }
    public static enum LossCounter {
    TOTAL_LOSS
    }
    public static class KMeansReducer extends Reducer<Text, Text, Text, Text> {
	private double totalLoss = 0.0;
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            double[][] sum = new double[VECTOR_SIZE][K];
            int[] count = new int[K];
	        double[] vectorTf_Idf_Double = new double[VECTOR_SIZE];
            for (Text value : values) {
                int centroidIndex = Integer.parseInt(key.toString());
                Arrays.fill(vectorTf_Idf_Double, 0.0);

                String[] parts = value.toString().split("\\s+");
                String vectorTf_Idf = parts[1];
                String[] vector_string = vectorTf_Idf.split(",");

                for (String termStr : vector_string) {
                    String[] term = termStr.split(":");
                    int term_id = Integer.parseInt(term[0]);
                    double tf_idf = Double.parseDouble(term[1]);
                    vectorTf_Idf_Double[term_id] = tf_idf;
                    sum[term_id][centroidIndex] += tf_idf;
                }
                count[centroidIndex]++;
            }

            for (int i = 0; i < K; i++) {
                if (count[i] > 0) {
                    String str = "";
                    double[] newCentroid = new double[VECTOR_SIZE];
                    for (int j = 0; j < VECTOR_SIZE; j++) {
                        newCentroid[j] = sum[j][i] / count[i];
                        str = str + "," + newCentroid[j];
                    }
		    double loss = calculateLoss(newCentroid, vectorTf_Idf_Double);
		    totalLoss += loss;
		    context.getCounter(LossCounter.TOTAL_LOSS).increment((long) (loss * 1000));
                    context.write(key, new Text(str));
                }
            }

        }

        private double calculateLoss(double[] newCentroid, double[] vectorTf_Idf_Double) {
            double loss = 0.0;
            for (int i = 0; i < VECTOR_SIZE; i++) {
                if (vectorTf_Idf_Double[i] != 0) {
                loss += Math.pow(vectorTf_Idf_Double[i] - newCentroid[i], 2);
                }
		    }
		    return loss;
    	}
	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
	    context.write(new Text("Total Loss:"), new Text(String.valueOf(totalLoss)));
	}
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Path inputPath = new Path(args[0]);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Job job = Job.getInstance(conf, "KMeans MapReduce");
            job.setJarByClass(KMeansMapReduce.class);
            job.setMapperClass(KMeansMapper.class);
            job.setReducerClass(KMeansReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);
            FileInputFormat.addInputPath(job, inputPath);

            Path outputPath = new Path("Iteration_" + (i + 1));
            FileOutputFormat.setOutputPath(job, outputPath);

            if (!job.waitForCompletion(true)) {
                System.exit(1);
            }

            Path centroidOutputPath = new Path("task_2_2.clusters");
            FileSystem fs = FileSystem.get(conf);
            fs.copyToLocalFile(new Path(outputPath, "part-r-00000"), centroidOutputPath);

            // Output mean of each cluster to task_2_2.txt
            outputClusterMeans(job, conf, centroidOutputPath, i + 1);
	    Path lossOutputPath = new Path("Iteration_" + (i + 1) + "/task_2_2.loss");
	    long totalLossCount = job.getCounters().findCounter(LossCounter.TOTAL_LOSS).getValue();
		double totalLoss = totalLossCount / 1000.0; // Scale back to original
		// Write total loss to output path
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(lossOutputPath)))) {
		    writer.write("Total Loss: " + totalLoss);
		}
        }
        System.exit(0);
    }

    private static void outputClusterMeans(Job job, Configuration conf, Path centroidOutputPath, int iteration) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(centroidOutputPath.toString()))) {
            String line;
            double[] tf_idf_mean = new double[VECTOR_SIZE];
            List<String> means = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                StringBuilder strBuilder = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    tf_idf_mean[i - 1] = Double.parseDouble(parts[i]);
                }
                Integer[] indices = new Integer[VECTOR_SIZE];
                for (int i = 0; i < VECTOR_SIZE; i++) {
                    indices[i] = i;
                }
                Arrays.sort(indices, Comparator.comparingDouble((Integer i) -> tf_idf_mean[i]).reversed());
                for (int i = 0; i < 10; i++) {
                    int index = indices[i];
                    double value = tf_idf_mean[index];
                    strBuilder.append(",").append(indices[i]).append(":").append(value);
                }
                means.add(strBuilder.toString());
            }

            FileSystem fs = FileSystem.get(conf);
            Path outputPath = new Path("Iteration_" + iteration + "/task_2_2.txt");
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(outputPath)))) {
                for (String mean : means) {
                    writer.write(mean);
                    writer.newLine();
                }
            }
        }
    }

}

