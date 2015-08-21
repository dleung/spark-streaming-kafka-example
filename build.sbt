lazy val commonSettings = Seq(
  organization := "org.myproject",
  version := "0.1.0",
  // set the Scala version used for the project.  2.11 isn't supported with Spark yet
  scalaVersion := "2.10.4"
)

val spark = "org.apache.spark" % "spark-core_2.10" % "1.4.1"
val sparkStreaming = "org.apache.spark" % "spark-streaming_2.10" % "1.4.1"
val sparkStreamKafka = "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.4.1"

// Needed to be able to parse the generated avro JSON schema
val jacksonMapperAsl = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.13"

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies += spark,
    libraryDependencies += sparkStreaming,
    libraryDependencies += sparkStreamKafka,
    libraryDependencies += jacksonMapperAsl
  )