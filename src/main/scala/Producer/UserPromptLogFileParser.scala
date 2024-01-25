package Producer

import org.json4s.DefaultFormats
import org.json4s.native.Serialization

import scala.io.Source
import scala.util.parsing.combinator.RegexParsers

case class UserPromptLogEntry(
  ipAddress: String,
  dateTime: String,
  prompt: String,
  userAgent: String,
  responseTime: Int
)

class UserPromptLogFileParser extends RegexParsers {

  // Define the log format using parser combinators
  def ipAddress: Parser[String] = """\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""".r
  def dash: Parser[String] = "-"
  def dateTime: Parser[String] = """\[(\d{2}/\w+/\d{4}:\d{2}:\d{2}:\d{2} \+\d{4})\]""".r ^^ {
    _.replaceAll("\\[", "").replaceAll("\\]", "")
  }
  def prompt: Parser[String] = """".*"""".r ^^ { str =>
    str.replace("\" \"", "").replace("\"", "")
  }
  def userAgent: Parser[String] = """".*"""".r ^^ { str =>
    str.replace("\" \"", "").replace("\"", "")
  }
  def responseTime: Parser[Int] = """\d+""".r ^^ { _.toInt }

  def userPromptLogEntry: Parser[UserPromptLogEntry] =
    ipAddress ~ dash ~ dash ~ dateTime ~ prompt ~ userAgent ~ responseTime ^^ {
      case ip ~ _ ~ _ ~ time ~ prompt ~ ua ~ rt =>
        UserPromptLogEntry(ip, time, prompt, ua, rt)
    }

  def parseUserPromptLogFile(logFile: String): Seq[String] = {
    val logEntries = Source.fromFile(logFile).getLines().flatMap { line =>
      parse(userPromptLogEntry, line) match {
        case Success(logEntry, _) => Some(logEntry)
        case Error(msg, _) => {
          println(s"Error while parsing log entry: $msg")
          None
        }
        case Failure(msg, _) => {
          println(s"Failed to parse log entry: $msg")
          None
        }
      }
    }.toSeq

    // Convert log entries to JSON strings
    implicit val formats: DefaultFormats = DefaultFormats
    logEntries.map(Serialization.write(_))
  }
}
