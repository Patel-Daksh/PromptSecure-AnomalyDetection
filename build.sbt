ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "Prompt Secure Anomaly Detection"
  )

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "2.8.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.json4s" %% "json4s-jackson" % "3.6.11",
  "org.json4s" %% "json4s-native" % "3.6.11",
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)