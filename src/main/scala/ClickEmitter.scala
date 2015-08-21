package main.scala

import java.io.ByteArrayOutputStream
import java.util

import events.avro.ClickEvent
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import _root_.scala.util.Random

/**
 * Run this to start emitting click events with:
 * sbt 'run-main main.scala.ClickEmitter localhost:9092 test'  -Dsbt.parser.simple=true
 */
object ClickEmitter extends App {
  if (args.length < 2) {
    System.err.println("Usage: KafkaWordCountProducer <metadataBrokerList> <topic>")
    System.exit(1)
  }

  val Array(brokers, topic) = args

  // Zookeeper connection properties
  val props = new util.HashMap[String, Object]()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)

  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
    "org.apache.kafka.common.serialization.ByteArraySerializer") // Kafka avro message stream comes in as a byte array
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
    "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, Array[Byte]](props)

  // Send some kafka click events at 1 second intervals
  while(true) {
    val clickBytes = serializeClickEvent(newRandomClickEvent) // Avro schema serialization as a byte array

    val message = new ProducerRecord[String, Array[Byte]](topic, null, clickBytes) // Create a new producer record to send the message in
    println("Click!")
    producer.send(message)

    Thread.sleep(1000)
  }

  // Generate a random click event
  def newRandomClickEvent: ClickEvent = {
    val userId = Random.nextInt(5) // We'll define 0..5 with valid mappings to real names
    val productId = Random.nextInt(5) // We'll define 0..5 with valid mappings to real product names.
    new ClickEvent(userId, productId)
  }

  // Serialize a click event using Avro into a byte array to send through Kafka
  // https://cwiki.apache.org/confluence/display/AVRO/FAQ
  def serializeClickEvent(clickEvent: ClickEvent): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get.binaryEncoder(out, null)
    val writer = new SpecificDatumWriter[ClickEvent](ClickEvent.getClassSchema)

    writer.write(clickEvent, encoder)
    encoder.flush
    out.close
    out.toByteArray
  }
}
