import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
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

    public static class AssignClusterMapper extends Mapper<Object, Text, Text, Text> {
        private List<Point> clusters = new ArrayList<>();

		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			FileSystem fs = FileSystem.get(conf);
            Path centroidPath = new Path(conf.get("path"));
            FSDataInputStream inputStream = fs.open(centroidPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                   int clusterClass = Integer.parseInt(parts[0]);
                   double x1 = Double.parseDouble(parts[1]);
                   double x2 = Double.parseDouble(parts[2]);
                   clusters.add(new Point(clusterClass, x1, x2));
                }
            }
			br.close();
		}

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			if (!line.startsWith("class")) {
				String[] parts = line.split(",");
				if (parts.length == 3) {
				    int pointClass = Integer.parseInt(parts[0].trim());
				    double x1 = Double.parseDouble(parts[1].trim());
				    double x2 = Double.parseDouble(parts[2].trim());
				    Point point = new Point(pointClass, x1, x2);
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
				        context.write(new Text(String.valueOf(nearestCluster.getClusterClass())), new Text("," + x1 + "," + x2));
				    }
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
        private double x1;
        private double x2;
        
        public Point(int clusterClass, double x1, double x2) {
            this.clusterClass = clusterClass;
            this.x1 = x1;
            this.x2 = x2;
        }
        
        public int getClusterClass() {
            return clusterClass;
        }
        
        public double getX1() {
            return x1;
        }
        
        public double getX2() {
            return x2;
        }
        
        public double distance(Point other) {
            return Math.sqrt(Math.pow(x1 - other.getX1(), 2) + Math.pow(x2 - other.getX2(), 2));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String centroidPath = args[1] + "/iteration_20/part-r-00000";
        conf.set("path", centroidPath);

        Job job = Job.getInstance(conf, "Assign Clusters");
        job.setJarByClass(AssignCluster.class);
        job.setMapperClass(AssignClusterMapper.class);
        job.setReducerClass(AssignClusterReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

