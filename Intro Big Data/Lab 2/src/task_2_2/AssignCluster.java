import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class AssignCluster {
    static final int VECTOR_SIZE = 10000;
    
    public static class AssignClusterMapper extends Mapper<Object, Text, Text, Text> {
        private List<Point> clusters = new ArrayList<>();

		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			FileSystem fs = FileSystem.get(conf);
			BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(new Path("/Lab/task_2_2.clusters"))));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                double[] x = new double[parts.length - 1];
                int clusterClass = Integer.parseInt(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    x[i - 1] = Double.parseDouble(parts[i]);
                }
                clusters.add(new Point(clusterClass, x));
                
            }
			br.close();
		}

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			if (!line.startsWith("class")) {
				
			    
			    double[] vectorTf_Idf_Double = new double[VECTOR_SIZE];
			    for (int i = 0; i < vectorTf_Idf_Double.length; i++) {
				vectorTf_Idf_Double[i] = 0.0;
			    }

			    String[] parts = value.toString().split("\\s+");
			    int docIdInt = Integer.parseInt(parts[0]);
			    String vectorTf_Idf = parts[1];
			    String[] vector_string = parts[1].split(",");
			
			    for (int i = 0; i < vector_string.length; i++) {
				String[] term = vector_string[i].split(":");
				int term_id = Integer.parseInt(term[0]);
				double tf_idf = Double.parseDouble(term[1]);
				vectorTf_Idf_Double[term_id] = tf_idf;
			    }
			    Point point = new Point(docIdInt, vectorTf_Idf_Double);
			    
			    Point nearestCluster = null;
		            double minDistance = Double.MAX_VALUE;
		            for (Point cluster : clusters) {
				double distance = point.distance(cluster);
				if (distance < minDistance) {
				   minDistance = distance;
				   nearestCluster = cluster;
				}
		            }
			       
				    if (nearestCluster != null) {
				        context.write(new Text(String.valueOf(nearestCluster.getClusterClass())), new Text(vectorTf_Idf));
				    }
			}
			
        }
    }
    
    public static class AssignClusterReducer extends Reducer<Text, Text, Text, Text> {
        
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text cluster : values) {
                context.write(key, cluster);
            }
        }
    }
    
    public static class Point {
        private int clusterClass;
        private double[] x = new double[VECTOR_SIZE];
        
        public Point(int clusterClass, double[] x) {
            this.clusterClass = clusterClass;
            this.x = x;
        }
        
        public int getClusterClass() {
            return clusterClass;
        }
        
        public double[] getX() {
            return x;
        }
        
        public double distance(Point other) {
            return cosineSimilarity(x, other.getX());
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

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Assign Clusters");
        job.setJarByClass(AssignCluster.class);
        job.setMapperClass(AssignClusterMapper.class);
        job.setReducerClass(AssignClusterReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}


