import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class triangleInUndirectedGraph {
	public static class CountVirtualEdgeMapper extends
			Mapper<LongWritable, Text, LongWritable, LongWritable> {
		Text mKey = new Text();
		LongWritable mValue = new LongWritable();

		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();

			mValue.set(Long.parseLong(line));
			context.write(new LongWritable(0), mValue);
		}
	}

	public static class CountTriangleCombiner extends
			Reducer<LongWritable, LongWritable, LongWritable, LongWritable> {
		@Override
		protected void reduce(LongWritable key, Iterable<LongWritable> values,
				Context context) throws IOException, InterruptedException {
			long sum = 0;
			for (LongWritable v : values) {
				sum += v.get();
			}
			context.write(key, new LongWritable(sum));
		}
	}

	public static class CountTriangleReducer extends
			Reducer<LongWritable, LongWritable, LongWritable, LongWritable> {
		@Override
		protected void reduce(LongWritable key, Iterable<LongWritable> values,
				Context context) throws IOException, InterruptedException {
			long sum = 0;
			for (LongWritable v : values) {
				sum += v.get();
			}
			context.write(new LongWritable(sum), null);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();

		Job job1 = new Job(conf, "triangle");
		job1.setMapOutputKeyClass(LongWritable.class);// 必须写，因为与OutPut不一致
		job1.setMapOutputValueClass(LongWritable.class);

		job1.setOutputKeyClass(LongWritable.class);
		job1.setOutputValueClass(LongWritable.class);

		job1.setJarByClass(triangleInUndirectedGraph.class);
		job1.setMapperClass(CountVirtualEdgeMapper.class);
		job1.setCombinerClass(CountTriangleCombiner.class);
		job1.setReducerClass(CountTriangleReducer.class);

		job1.setInputFormatClass(TextInputFormat.class);
		job1.setOutputFormatClass(TextOutputFormat.class);

		job1.setNumReduceTasks(1);

		FileInputFormat.addInputPath(job1, new Path(args[1]
				+ "/countvirtualedge"));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]
				+ "/trianglesInUndirectedGraph"));
		System.exit(job1.waitForCompletion(true) ? 0 : 1);

	}

}
