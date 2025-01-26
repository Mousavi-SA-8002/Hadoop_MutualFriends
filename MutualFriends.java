import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MutualFriends {

    public static class FriendsMapper extends Mapper<Object, Text, Text, Text> {
        private Text userPair = new Text();
        private Text friendsList = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] line = value.toString().split("\\s+");
            if (line.length < 2) return;

            String user = line[0];
            String[] friends = line[1].split(",");

            for (String friend : friends) {
                String pairKey = user.compareTo(friend) < 0 ? user + "," + friend : friend + "," + user;
                userPair.set(pairKey);
                friendsList.set(line[1]);
                context.write(userPair, friendsList);
            }
        }
    }

    public static class FriendsReducer extends Reducer<Text, Text, Text, Text> {
        private Text mutualFriends = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Set<String> set1 = new HashSet<>(Arrays.asList(values.iterator().next().toString().split(",")));
            if (values.iterator().hasNext()) {
                Set<String> set2 = new HashSet<>(Arrays.asList(values.iterator().next().toString().split(",")));
                set1.retainAll(set2);
                mutualFriends.set(String.join(",", set1));
                context.write(key, mutualFriends);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Mutual Friends");
        job.setJarByClass(MutualFriends.class);
        job.setMapperClass(FriendsMapper.class);
        job.setReducerClass(FriendsReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
