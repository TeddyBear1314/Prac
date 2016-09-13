import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class CountVirtualEdge {

	public static class CountVirtualEdgeMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		Text mKey = new Text();
		Text mValue = new Text();

		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String[] line = value.toString().split("\t");
			mKey.set(line[0]);
			mValue.set(line[1]);
			context.write(mKey, mValue);
		}
	}

	public static class CountVirtualEdgeCombiner extends
			Reducer<Text, Text, Text, Text> {
		String value1 = null;
		Text mValue = new Text();

		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {

			StringBuffer value2 = new StringBuffer();
			for (Text v : values) {
				String[] res = v.toString().split(" ", 2);
				if (res[0].equals("v")) {
					value2.append(" " + res[1]);
				} else {
					value1 = v.toString();
				}
			}
			if (value1 != null && value2.length() > 0)
				mValue.set(value1 + " v" + value2.toString());
			else if (value1 != null)
				mValue.set(value1);
			else
				mValue.set("v" + value2.toString());

			context.write(key, mValue);

		}
	}

	public static class CountVirtualEdgeReducer extends
			Reducer<Text, Text, LongWritable, LongWritable> {
		Map<String, Long> vEdge;
		Set<String> rEdge;
		long cnt = 0;

		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			vEdge = new HashMap<String, Long>();
			rEdge = new HashSet<String>();
			for (Text v : values) {
				String[] res = v.toString().split(" ", 2);
				if (res[0].equals("v")) {
					String[] res2 = res[1].split(" ");
					for (String vedge : res2) {
						if (vEdge.get(vedge) == null)
							vEdge.put(vedge, (long) 1);
						else
							vEdge.put(vedge, vEdge.get(vedge) + 1);
					}

				} else {
					String[] res2 = res[1].split(" ");
					boolean flag = false;
					for (String edge : res2) {
						if (edge.equals("v")) {
							flag = true;
							continue;
						}
						if (flag) {
							if (vEdge.get(edge) == null)
								vEdge.put(edge, (long) 1);
							else
								vEdge.put(edge, vEdge.get(edge) + 1);
						} else
							rEdge.add(edge);
					}

				}

			}

			Set<String> vE = vEdge.keySet();
			for (String v2 : vE) {
				if (rEdge.contains(v2))
					cnt += vEdge.get(v2);
				;
			}
		}

		@Override
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			context.write(new LongWritable(cnt), null);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		// 命令行参数
		Job job1 = new Job(conf, "CountVirtualEdge");
		job1.setMapOutputKeyClass(Text.class);// 必须写，因为与OutPut不一致
		job1.setMapOutputValueClass(Text.class);

		job1.setOutputKeyClass(LongWritable.class);
		job1.setOutputValueClass(LongWritable.class);

		job1.setJarByClass(CountVirtualEdge.class);
		job1.setMapperClass(CountVirtualEdgeMapper.class);
		job1.setReducerClass(CountVirtualEdgeReducer.class);

		job1.setInputFormatClass(TextInputFormat.class);
		job1.setOutputFormatClass(TextOutputFormat.class);

		job1.setNumReduceTasks(50);
		FileInputFormat.addInputPath(job1, new Path(args[1]
				+ "/undirectedgraph"));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]
				+ "/countvirtualedge"));
		job1.waitForCompletion(true);
	}
}
