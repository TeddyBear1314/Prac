import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class UndirectedGraph {
	public static class UndirectedMapper // ����Map��ʵ���ַ����ֽ⣬
			extends Mapper<LongWritable, Text, Text, Text>// ʵ��������int���²���
	{
		Text mKey = new Text();
		Text mValue = new Text();

		 static Map<String,Integer> outdegree = new HashMap<String,Integer>();
		 @Override
		 protected void setup(Context context) throws IOException,InterruptedException {
		 Path[] cacheFiles = DistributedCache.getLocalCacheFiles(context.getConfiguration());//����ָDIstributedCache�ϵĶ��·��,cacheFiles�ǿյ�
		 FileSystem fsopen= FileSystem.getLocal(context.getConfiguration());
		 for(Path p:cacheFiles){
		FSDataInputStream in = fsopen.open(p);   
		BufferedReader bw = new BufferedReader(new InputStreamReader(in));
		 String line;
		 String[] tokens;
		 while((line = bw.readLine())!=null){
		 tokens = line.split("\t");
		  if(outdegree.get(tokens[0])==null){
			  outdegree.put(tokens[0], Integer.parseInt(tokens[1]));
		  }
		  else{
			  outdegree.put(tokens[0], outdegree.get(tokens[0])+Integer.parseInt(tokens[1]));
		  }
		 }
		 }
					
		 }

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
			if (outdegree.get(e1) < outdegree.get(e2)) {
				mKey.set(e1);
				mValue.set(e2);
				context.write(mKey, mValue);
			} else if(outdegree.get(e1) > outdegree.get(e2)){
				mKey.set(e2);
				mValue.set(e1);
				context.write(mKey, mValue);
			}else {
				if(e1.compareTo(e2)<0){
					mKey.set(e1);
					mValue.set(e2);
					context.write(mKey, mValue);
				}else{
					mKey.set(e2);
					mValue.set(e1);
					context.write(mKey, mValue);
				}
				
			}
		}
	}

	public static class UndirectedCombiner extends
			Reducer<Text, Text, Text, Text> {
	
		Text mValue = new Text();
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

	public static class UndirectedReducer extends
			Reducer<Text, Text, Text, Text> {
		Text rKey = new Text();
		Text rValue = new Text();
		String[] vArray = new String[4096];// store the virtual edges

		 static Map<String,Integer> outdegree = new HashMap<String,Integer>();
		 @Override
		 protected void setup(Context context) throws IOException,InterruptedException {
		 Path[] cacheFiles = DistributedCache.getLocalCacheFiles(context.getConfiguration());//����ָDIstributedCache�ϵĶ��·��,cacheFiles�ǿյ�
		 FileSystem fsopen= FileSystem.getLocal(context.getConfiguration());
		 for(Path p:cacheFiles){
			 System.out.println(p);
		FSDataInputStream in = fsopen.open(p);   
		BufferedReader bw = new BufferedReader(new InputStreamReader(in));
		 String line;
		 String[] tokens;
		 while((line = bw.readLine())!=null){
		 tokens = line.split("\t");
		  if(outdegree.get(tokens[0])==null){
			  outdegree.put(tokens[0], Integer.parseInt(tokens[1]));
		  }
		  else{
			  outdegree.put(tokens[0], outdegree.get(tokens[0])+Integer.parseInt(tokens[1]));
		  }
		 }
		 }
					
		 }
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
		
			int size = 0;// the number of virtual edges,ÿ�ν�reduce��Ҫ��Ϊ0
			StringBuffer value = new StringBuffer();
			StringBuffer value2 = new StringBuffer();
			String[] res = sb.toString().split(" ");
			for (String v2 : res) {
				if (vArray.length == size)
					vArray = Arrays.copyOf(vArray, size * 2);// prevent
				// tuple�����
				String e = v2.toString();
				vArray[size++] = e;
				value.append(" " + e);				
			}
			rValue.set("r" + value.toString());
			context.write(key, rValue);
			_quickSort(vArray,0,size-1);// ��Ϊvalues��ֵ�ǲ�����ģ���������Ҫ�����key���ֵ����,�����size����Ϊʣ�µ������ǿյģ���NullPointerException
			
			for (int i = 0; i < size - 1; i++) {
				for (int j = i + 1; j < size; j++) {
					value2.append(" " + vArray[j]);

				}
				rKey.set(vArray[i]);
				rValue.set("v" + value2.toString());
				context.write(rKey, rValue);
				value2.setLength(0);
			}
		}
		private static void _quickSort(String[] list, int low, int high) {   

            if (low < high) {   

               int middle = getMiddle(list, low, high);  //��list�������һ��Ϊ��   

                _quickSort(list, low, middle - 1);        //�Ե��ֱ���еݹ�����   

               _quickSort(list, middle + 1, high);       //�Ը��ֱ���еݹ�����   

            }   

        } 

		private static int getMiddle(String[] list, int low, int high) {   

            String tmp = list[low];    //����ĵ�һ����Ϊ����   

            while (low < high) {   

                while (low < high && compare(list[high],tmp)>0) {   

                    high--;   

                }   

                list[low] = list[high];   //������С�ļ�¼�Ƶ��Ͷ�   

                while (low < high && compare(list[low],tmp)<0) {   

                    low++;   

                }   

                list[high] = list[low];   //�������ļ�¼�Ƶ��߶�   

            }   

           list[low] = tmp;              //�����¼��β   

            return low;                   //���������λ��   

        }  
          protected static  int compare(String a,String b){
        	 if(outdegree.get(a)<outdegree.get(b))
        		  return -1;
        	
        	  if(outdegree.get(a)>outdegree.get(b))
        		  return 1;
        	  else{
        		  if(a.compareTo(b)<0)
        			  return -1;
        		  else return 1;
        	  }
          }

		
}


	

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		
		// �����в���
		FileSystem hdfs = FileSystem.get(conf);
		Path inputDir = new Path(args[1] + "/outdegree");
		FileStatus[] inputFiles = hdfs.listStatus(inputDir);
		for (int i = 0; i < inputFiles.length; i++) {
			if (inputFiles[i].isDir())
				continue;
		if(inputFiles[i].getPath().toString().contains("_SUCCESS"))continue;
			
			DistributedCache.addCacheFile(inputFiles[i].getPath().toUri(), conf);
		}
		Job job1 = new Job(conf, "UndirectedGraph");
		job1.setMapOutputKeyClass(Text.class);// ����д����Ϊ��OutPut��һ��
		job1.setMapOutputValueClass(Text.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(Text.class);

		job1.setJarByClass(UndirectedGraph.class);
		job1.setMapperClass(UndirectedMapper.class);
		job1.setCombinerClass(UndirectedCombiner.class);
		job1.setReducerClass(UndirectedReducer.class);

		job1.setInputFormatClass(TextInputFormat.class);
		job1.setOutputFormatClass(TextOutputFormat.class);

		job1.setNumReduceTasks(20);

		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]
				+ "/undirectedgraph"));
		job1.waitForCompletion(true);
	}
}
