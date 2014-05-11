package helpers

import models.Repeat
import org.joda.time.{DateTimeConstants, LocalDate, LocalTime, DateTime}
import models.Repeat.Frequency
import scala.util.matching.Regex
import play.Logger
import java.lang.reflect.InvocationTargetException
import org.joda.time.format.DateTimeFormat


object ReminderParsing {

  sealed abstract class Parsed {
    def isParsedSuccessfully = false
  }
  case class Success(what: String, firstTime: DateTime, repeat: Frequency) extends Parsed {
    override def isParsedSuccessfully = true
  }
  case object Failure extends Parsed
  case object DateTooEarly extends Parsed
  case object InvalidDate extends Parsed
  case object NoWhat extends Parsed

  val pattern = new Regex("(?i)@RemindTweets Remind Me (to)? (.+?)\\s*(on (.+?)?)?\\s*(at (.+?)?)?\\s*(every (.+?)?)?$",
    "to", "what", "on", "when", "at", "time", "every", "repeat")


  def getResultOfReminderRegex(text: String): Option[Regex.Match] = {
    pattern.findFirstMatchIn(text)
  }

  def convertRegexToGroupMap(matched: Regex.Match): Map[String,String] = {
    (for ((name, group) <- matched.groupNames zip matched.subgroups if group != null) yield name -> group).toMap
  }

  /**
   * Return a map representing the matched groups of the regex or,
   * if the input regex doesn't match, return an empty map
   * @param text
   * @return
   */
  def getStructuredReminderResult(text: String): Map[String,String] = {
    val result = getResultOfReminderRegex(text)
    if (result.isDefined) {
      convertRegexToGroupMap(result.get)
    } else {
      Map[String,String]()
    }
  }

  /**
   *
   * The main method that converts a tweet into the
   * logical contents of a reminder
   * Return None if the parsing fails or if the tweet
   * doesn't match the structure of an acceptable reminder
   * @param text
   * @return
   */
  def parseStatusText(text: String): ReminderParsing.Parsed = {


    Logger.info("Checking text: {}", text)

    val result = getResultOfReminderRegex(text)
    if(result.isEmpty) {
      Logger.info("Didn't match pattern: {}", text)
      return ReminderParsing.Failure
    }

    Logger.info("Found match pattern: {}", text)

    val groupMap = convertRegexToGroupMap(result.get)

    val what = groupMap.get("what")

    if (what.isEmpty) {
      return ReminderParsing.NoWhat
    }

    val repeat = getRepeatFrequency(groupMap.get("repeat"))
    val parsedTime = parseReminderTime(groupMap.get("time"), groupMap.get("when"))

    parsedTime match {
      case None =>
        Logger.error("Failed to parse time {}", groupMap("time"))
        ReminderParsing.Failure
      case Some(time) =>
        if (time.isAfter(DateTime.now())) {
          ReminderParsing.Success(what.get, time, repeat)
        } else {
          ReminderParsing.DateTooEarly
        }
    }
  }


  def getRepeatFrequency(repeat: Option[String]): Frequency = {

    if (repeat.isEmpty) {
      return Repeat.Never
    }

    val freq = repeat.get

    val weekly = """(?i).*week.*""".r

    freq match {
      case weekly() => Repeat.Weekly
      case _ => try {
        Repeat.withName(freq)
      } catch {case e: InvocationTargetException => Repeat.Never
      }
    }
  }

  /**
   * Takes a string and returns the
   * @param timeString
   * @return
   */
  def parseReminderTime(timeString: String) : Option[DateTime] = {

    // Check if it's a pure DateTime
    try {
      return Some(DateTime.parse(timeString))
    } catch { case e: Exception =>  }

    val twelveHour = parseTwelveHour(timeString)

    if (twelveHour.isDefined) {
      return Some(DateTime.now().withTime(twelveHour.get.getHourOfDay, twelveHour.get.getMinuteOfHour,
        twelveHour.get.getSecondOfMinute,twelveHour.get.getMillisOfSecond))
    }

    None
  }

  def parseReminderTime(time: Option[String], when: Option[String]) : Option[DateTime] = {
    if (time.isDefined && when.isDefined) {
      Some(parseTimeAndDate(time.get, when.get))
    }
    else if (time.isDefined) {
      Some(parseTimeTodayOrTomorrow(time.get))
    }
    else if (when.isDefined) {
      Some(parseDate(when.get).toDateTimeAtStartOfDay.withTime(12,0,0,0))
    }
    else {
      None
    }
  }


  def parseTimeAndDate(timeString: String, dateString: String): DateTime = {

    // First, parse the time of day string
    val timeOfDay: LocalTime = try {
      parseTwelveHour(timeString).get
    } catch {
      case e: Exception => DateTime.now().toLocalTime
    }

    // Then, parse the date
    val date: LocalDate = parseDate(dateString)

    date.toDateTimeAtStartOfDay.withTime(timeOfDay.getHourOfDay, timeOfDay.getMinuteOfHour,
      timeOfDay.getSecondOfMinute, timeOfDay.getMillisOfSecond)
  }


  def parseDate(dateString: String): LocalDate = {
    dateString match {
      case "Today" => LocalDate.now()
      case "Tomorrow" => LocalDate.now().plusDays(1)
      case "Monday" => getNextDayOfWeek(DateTimeConstants.MONDAY)
      case "Tuesday" => getNextDayOfWeek(DateTimeConstants.TUESDAY)
      case "Wednesday" => getNextDayOfWeek(DateTimeConstants.WEDNESDAY)
      case "Thursday" => getNextDayOfWeek(DateTimeConstants.THURSDAY)
      case "Friday" => getNextDayOfWeek(DateTimeConstants.FRIDAY)
      case "Saturday" => getNextDayOfWeek(DateTimeConstants.SATURDAY)
      case "Sunday" => getNextDayOfWeek(DateTimeConstants.SUNDAY)
      case _ => LocalDate.now()
    }
  }

  def getNextDayOfWeek(dayOfWeek: Int) = {
    val d: LocalDate = LocalDate.now().withDayOfWeek(dayOfWeek)
    if (d.isBefore(LocalDate.now())) {
      d.plusWeeks(1)
    } else {
      d
    }
  }

  def parseTimeTodayOrTomorrow(timeString: String): DateTime = {

    val time: LocalTime = parseTwelveHour(timeString).get

    val timeAtToday = setTime(DateTime.now(), time)
    if (timeAtToday.isAfter(DateTime.now())) {
      timeAtToday
    } else {
      setTime(DateTime.now().plusDays(1), time)
    }
  }

  def setTime(dateTime: DateTime , time: LocalTime) = {
    dateTime.withTime(time.getHourOfDay, time.getMinuteOfHour, time.getSecondOfMinute, time.getMillisOfSecond)
  }

  def parseTwelveHour(time: String): Option[LocalTime] = {

    Logger.info("Parsing Time: {}", time)

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("hh:mm aa")))
    } catch { case e: java.lang.IllegalArgumentException => }

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("hh:mmaa")))
    } catch { case e: java.lang.IllegalArgumentException => }

    None
  }
}

