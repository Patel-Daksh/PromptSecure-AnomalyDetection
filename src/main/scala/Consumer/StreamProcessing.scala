package Consumer

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.col

case class KafkaMessage(ipAddress: String, dateTime: String, prompt: String, userAgent: String, responseTime: Int) extends Serializable

object StreamProcessing {

  def classifyAnomalies(df: DataFrame, keywordList: List[String]) = {
    // Convert the keyword list to lowercase for case-insensitive matching
    val lowercaseKeywords = keywordList.map(_.toLowerCase)

    // Construct a regular expression pattern using the keywords
    val keywordPattern = lowercaseKeywords.mkString("|")

    // Filter anomalies based on the constructed pattern
    df.filter(lower(col("prompt")).rlike(keywordPattern))
  }

  def main(args: Array[String]): Unit = {

    // Set the logger level to error
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("akka").setLevel(Level.ERROR)

    // Create a Spark configuration
    val conf = new SparkConf()
      .setAppName("StreamingApp")
      .setMaster("local[*]")

    // Create a Spark Streaming Context
    val ssc = new StreamingContext(conf, Seconds(1))

    // Define Kafka parameters
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "group.id" -> "test-consumer-group",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (true: java.lang.Boolean)
    )

    // Define topics to read from
    val topics = Array("userpromptlogs")

    // Create a Kafka DStream
    val kafkaStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaParams)
    )

    kafkaStream.foreachRDD { rdd =>
      if (!rdd.isEmpty) {
        val spark = SparkSession.builder.config(rdd.sparkContext.getConf).getOrCreate()
        import spark.implicits._
        val df = rdd.map(record => {
          val jsonString = record.value()
          implicit val formats = DefaultFormats
          val kafkaMessage = parse(jsonString).extract[KafkaMessage]
          kafkaMessage
        }).toDF()

        // List of keywords to detect anomalies
        val keywordList = List("malicious", "suspicious", "fraud", "security_alert", "anomaly")

        val anomalyDF = classifyAnomalies(df, keywordList)
        anomalyDF.collect().foreach { row =>
          // Send anomalies to ElasticSearch
          SendToElastic.send(row)

          // Alert the user
          SendEmail.send(row)
        }
      }
    }

    // Start the streaming context
    ssc.start()

    // Wait for the streaming context to terminate
    ssc.awaitTermination()
  }
}
