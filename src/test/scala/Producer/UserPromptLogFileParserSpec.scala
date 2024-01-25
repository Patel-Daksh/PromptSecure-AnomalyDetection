package Producer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserPromptLogFileParserSpec extends AnyFlatSpec with Matchers {
  val logFileParser = new UserPromptLogFileParser()

  "parseUserPromptLogFile" should "return an empty list for an empty log file" in {
    val logFile = "src/main/resources/userprompts.log"
    logFileParser.parseUserPromptLogFile(logFile) should be(empty)
  }

  "UserPromptLogFileParser" should "parse all the log entries" in {
    val logFile = "src/main/resources/userprompts.log"
    val parsedLogEntries = logFileParser.parseUserPromptLogFile(logFile)

    parsedLogEntries should not be empty
    assert(parsedLogEntries.length === 1000000)
  }

  it should "handle invalid log entries" in {
    val invalidLogEntry = "192.168.0.1 - - [12/Jan/2021:10:32:11 +0000] \"Some invalid prompt\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64)\" invalid_response_time"
    val parsed = logFileParser.parse(logFileParser.userPromptLogEntry, invalidLogEntry)

    parsed.successful shouldEqual false
  }

  it should "return a list of JSON strings for a non-empty log file" in {
    val logFile = "src/main/resources/userprompts.log"
    val jsonStrings = logFileParser.parseUserPromptLogFile(logFile)
    jsonStrings should not be empty
    jsonStrings.foreach { jsonString =>
      assert(jsonString.contains("ipAddress"))
      assert(jsonString.contains("dateTime"))
      assert(jsonString.contains("prompt"))
      assert(jsonString.contains("userAgent"))
      assert(jsonString.contains("responseTime"))
    }
  }
}
