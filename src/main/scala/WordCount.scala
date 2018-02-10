import java.net.InetSocketAddress

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper
import java.util.Arrays
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisClusterConfig
import java.util.LinkedHashSet

object WordCount {

  def main(args: Array[String]) {

    // Checking input parameters
    val params = ParameterTool.fromArgs(args)
    val input = params.getRequired("input")
    val output = params.getRequired("output")

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)

    //set up redis connection
//    val conf = new FlinkJedisPoolConfig.Builder().setHost(output).setPort(7000).build()
    val node1 = new InetSocketAddress(output, 7000);
    val node2 = new InetSocketAddress(output, 7001);
    val node3 = new InetSocketAddress(output, 7002);
    val node4 = new InetSocketAddress(output, 7003);
    val node5 = new InetSocketAddress(output, 7005);
    val node6 = new InetSocketAddress(output, 7006);
    val cluster = new LinkedHashSet[InetSocketAddress]();
    cluster.add(node1);
    cluster.add(node2);
    cluster.add(node3);
    cluster.add(node4);
    cluster.add(node5);
    cluster.add(node6);
    
    val conf = new FlinkJedisClusterConfig.Builder().setNodes(cluster).build()
    // get input data
    val text =
    // read the text file from given input path
      env.readTextFile(input)

    val counts: DataStream[(String, Int)] = text
      // split up the lines in pairs (2-tuples) containing: (word,1)
      .flatMap(_.toLowerCase.split("\\W+"))
      .filter(_.nonEmpty)
      .map((_, 1))
      // group by the tuple field "0" and sum up tuple field "1"
      .keyBy(0)
      .sum(1)

    
    counts.addSink(new RedisSink[(String, Int)](conf, new RedisExampleMapper))
    counts.writeAsText(output)
    counts.print()

    // execute program
    env.execute("Streaming WordCount")
  }
  
  

}
class RedisExampleMapper extends RedisMapper[(String, Int)]{
  override def getCommandDescription: RedisCommandDescription = {
    new RedisCommandDescription(RedisCommand.HSET, "HASH_NAME")
//    new RedisCommandDescription(RedisCommand.SET, null)
  }

  override def getKeyFromData(data: (String, Int)): String = data._1

  override def getValueFromData(data: (String, Int)): String = data._2.toString
}