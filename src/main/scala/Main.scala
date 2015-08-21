package main.scala

import events.avro.ClickEvent
import kafka.serializer.DefaultDecoder
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}

object Main extends App {
  /**
   * Example Arguments: test 2
   */
  if (args.length < 2) {
    System.err.println("Usage: <topic> <numThreads>")
    System.exit(1)
  }

  println("Initializing App")

  val Array(topics, numThreads) = args

  val sparkConf = new SparkConf().setAppName("WindowClickCount").setMaster("local[2]")

  // Slide duration of ReduceWindowedDStream must be multiple of the parent DStream, and we chose 2 seconds for the reduced
  // window stream
  val ssc = new StreamingContext(sparkConf, Seconds(2))

  // Because we're using .reduceByKeyAndWindow, we need to persist it to disk
  ssc.checkpoint("./checkpointDir")

  val kafkaConf = Map(
    "metadata.broker.list" -> "localhost:9092", // Default kafka broker list location
    "zookeeper.connect" -> "localhost:2181", // Default zookeeper location
    "group.id" -> "kafka-spark-streaming-example",
    "zookeeper.connection.timeout.ms" -> "1000")

  val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap

  // Create a new stream which can decode byte arrays.  For this exercise, the incoming stream only contain user and product Ids
  val lines = KafkaUtils.createStream[String, Array[Byte], DefaultDecoder, DefaultDecoder](ssc, kafkaConf, topicMap, StorageLevel.MEMORY_ONLY_SER).map(_._2)

  // Create a RDD containing the mapping of ids to names.  In practice, these mappings will come from HDFS/S3/(a real data source)
  val userNameMapRDD = ssc.sparkContext.parallelize(Array((1,"Joe"), (2, "Michelle"), (3, "David"), (4, "Anthony"), (5, "Lisa")))
  val productNameMapRDD = ssc.sparkContext.parallelize(Array((1,"Legos"), (2, "Books"), (3, "Board Games"), (4, "Food"), (5, "Computers")))

  // Join the stream with the userNameMapRDD to map the userName to the userId
  val mappedUserName = lines.transform{rdd =>
    val clickRDD: RDD[(Int, Int)] = rdd.map { bytes => AvroUtil.clickEventDecode(bytes) }.map { clickEvent =>
      (clickEvent.getUserId: Int) -> clickEvent.getProductId
    }

    clickRDD.join(userNameMapRDD).map { case (userId, (productId, userName)) => (userName, productId)}
  }

  // Join the stream with the productNameMapRDD to map the productName to the productId
  val mappedProductId = mappedUserName.transform{ rdd =>
    val productRDD = rdd.map { case (userName, productId) => (productId: Int, userName) }

    productRDD.join(productNameMapRDD).map { case (productId, (productName, userName)) => (userName, productName)}
  }

  // Get a count of all the users and the products they visited in the last 10 minutes, refreshing every 2 seconds
  val clickCounts = mappedProductId.map(x => (x, 1L))
    .reduceByKeyAndWindow(_ + _, _ - _, Minutes(10), Seconds(2), 2).map { case ((productName, userName), count) =>
    (userName, productName, count)
  }

  clickCounts.print // Print out the results.  Or we can produce new kafka events containing the mapped ids.

  ssc.start()
  ssc.awaitTermination()
}

object AvroUtil {
  // Deserialize the byte array into an avro object
  // https://cwiki.apache.org/confluence/display/AVRO/FAQtil {
  val reader = new SpecificDatumReader[ClickEvent](ClickEvent.getClassSchema)
  def clickEventDecode(bytes: Array[Byte]): ClickEvent = {
    val decoder = DecoderFactory.get.binaryDecoder(bytes, null)
    reader.read(null, decoder)
  }
}