package Producer

object Main {
  def main(args: Array[String]): Unit = {
    val userPromptlogFileParser = new UserPromptLogFileParser()
    val logFile = "src/main/resources/userprompts.log"
    val logEntries = userPromptlogFileParser.parseLogFile(logFile)

    val kafkaMessageSender = new KafkaMessageSender("localhost:9092", "userpromptlogs")
    logEntries.foreach(logEntry => kafkaMessageSender.sendMessage(logEntry))

    kafkaMessageSender.close()
  }
}
