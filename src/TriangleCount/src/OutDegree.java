import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
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



public class OutDegree {

		public static class OutDegreeMapper // 定义Map类实现字符串分解，
		extends Mapper<LongWritable, Text, Text, Text>// 实际我想用int，怕不够
{
	Text mKey = new Text();
	Text mValue = new Text();

	@Override
	protected void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		String line = value.toString();
		StringTokenizer tokenizer = new StringTokenizer(line);
		String e1 = null, e2 = null;
		if (tokenizer.hasMoreTokens()) {
			e1 = tokenizer.nextToken();
			e2 = tokenizer.nextToken();
		}
		if (e1.compareTo(e2) < 0) {
			mKey.set(e1);
			mValue.set(e2);
			context.write(mKey, mValue);
		} else {
			mKey.set(e2);
			mValue.set(e1);
			context.write(mKey, mValue);
		}
	}
}

public static class OutDegreeCombiner extends
		Reducer<Text, Text, Text, Text> {
	Text mValue = new Text();
	IntWritable one = new IntWritable(1);
	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {
		StringBuffer sb = new StringBuffer();
		for (Text v : values) {
			String vertex = v.toString();
			sb.append(vertex + " ");

		}
		sb.deleteCharAt(sb.length() - 1);
		mValue.set(sb.toString());
		context.write(key, mValue);
	}
}

public static class OutDegreeReducer extends
		Reducer<Text, Text, Text, IntWritable> {
	Text rKey = new Text();
	IntWritable rValue = new IntWritable();
	Map<String,Integer> outdegree = new HashMap<String,Integer>();
	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {	
		Set<String> stat = new HashSet<String>();
		StringBuffer sb = new StringBuffer();
		for (Text v : values) {
			String[] res = v.toString().split(" ");
			for (String v2 : res) {
				if (!stat.contains(v2)) {
					sb.append(v2 + " ");
					stat.add(v2);
				}
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		
			String[] vertex = sb.toString().split(" ");
			String vertKey = key.toString();
			if(outdegree.get(vertKey)==null)outdegree.put(vertKey, vertex.length);
			else outdegree.put(vertKey, outdegree.get(vertKey)+vertex.length);
			for(String vert:vertex){
			if(outdegree.get(vert)==null)outdegree.put(vert, 1);
			else outdegree.put(vert, outdegree.get(vert)+1);
			}
		
	}
	@Override
	protected void cleanup(Context context) throws IOException,
			InterruptedException {
		Iterator<Map.Entry<String,Integer>> iter = outdegree.entrySet().iterator();
		while(iter.hasNext()){
			Map.Entry<String,Integer> entry = iter.next();
			rKey.set(entry.getKey());
			rValue.set(entry.getValue());
			context.write(rKey, rValue);
		}
	}
}

public static void main(String[] args) throws Exception {
	Configuration conf = new Configuration();
	// 命令行参数
	Job job1 = new Job(conf, "OutDegree");
	job1.setMapOutputKeyClass(Text.class);// 必须写，因为与OutPut不一致
	job1.setMapOutputValueClass(Text.class);

	job1.setOutputKeyClass(Text.class);
	job1.setOutputValueClass(Text.class);

	job1.setJarByClass(OutDegree.class);
	job1.setMapperClass(OutDegreeMapper.class);
	job1.setCombinerClass(OutDegreeCombiner.class);
	job1.setReducerClass(OutDegreeReducer.class);

	job1.setInputFormatClass(TextInputFormat.class);
	job1.setOutputFormatClass(TextOutputFormat.class);

	job1.setNumReduceTasks(20);

	FileInputFormat.addInputPath(job1, new Path(args[0]));
	FileOutputFormat.setOutputPath(job1, new Path(args[1]
			+ "/outdegree"));
	job1.waitForCompletion(true);
}

	

}
