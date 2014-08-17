package helpers

import models.Repeat
import org.joda.time._
import models.Repeat.Frequency
import scala.util.matching.Regex
import play.Logger
import java.lang.reflect.InvocationTargetException
import org.joda.time.format.DateTimeFormat

import com.joestelmach.natty.Parser

object ReminderParsing {

  val timeZone = DateTimeZone.forID("America/Los_Angeles")

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

  def convertRegexToGroupMap(matched: Regex.Match): Map[String,String] = {
    (for ((name, group) <- matched.groupNames zip matched.subgroups if group != null) yield name -> group).toMap
  }

  // Relative Time Non Recurring
  // Example: Remind me to WHAT in 4 hours.
  val patternA = new Regex("(?i)@RemindTweets Remind Me\\s+(to)?\\s*(.+)\\s+(in)\\s+(.+?)\\.?$",
    "to", "what", "in", "relativeTime")

  // Absolute Time With recurring
  // Example: "Remind me to WHAT on Tuesday at 6:00pm every week."
  val patternB = new Regex("(?i)@RemindTweets Remind Me\\s+(to)?\\s*(.+)\\s+(on\\s+(.+?))\\s*(at\\s+(.+?))\\s*(every\\s+(.+?))\\.?$",
    "to", "what", "on", "when", "at", "time", "every", "repeat")

  // Absolute Time With recurring
  // Example: "Remind me to WHAT on Tuesday at 6:00pm."
  val patternC = new Regex("(?i)@RemindTweets Remind Me\\s+(to)?\\s*(.+)\\s+(on\\s+(.+?))\\s+(at\\s+(.+?))\\s*\\.?$",
    "to", "what", "on", "when", "at", "time")

  // Absolute Time With recurring
  // Example: "Remind me to WHAT on Tuesday at 6:00pm."
  val patternD = new Regex("(?i)@RemindTweets Remind Me\\s+(to)?\\s*(.+)\\s+(at\\s+(.+?)?)\\s*\\.?$",
    "to", "what", "at", "time")


  /**
   * Takes a string status text and parses it into a map
   * of data that can be used to create a reminder, or
   * return None if not matching parse can be made.
   * No validation on the data is made, this only
   * matches the strings against acceptable patterns
   * @param text
   * @return
   */
  def parseStatusTextIntoReminderData(text: String): Option[Map[String,String]] = {

    val patterns = List(patternA, patternB, patternC, patternD)

    // Go most specific to least specific
    for (pattern <- patterns) {
      pattern.findFirstMatchIn(text) match {
        case Some(group) =>
          Logger.debug("Text {} matches pattern: {}", text, pattern)
          return Some(convertRegexToGroupMap(group))
        case None =>
          Logger.debug("Text {} does not match pattern: {}", text, pattern)
      }
    }

    None
  }


  /**
   * Takes a map of data from parsing a tweet
   * and converts it into a valid reminder,
   * returning errors for invalid data
   * @param groupMap
   * @param createdAt
   * @return
   */
  def parseReminderDataIntoReminder(groupMap: Map[String,String], createdAt: DateTime): ReminderParsing.Parsed = {

    val what = groupMap.get("what")
    if (what.isEmpty) {
      return ReminderParsing.NoWhat
    }

    val repeat = getRepeatFrequency(groupMap.get("repeat"))

    val firstTime = if (groupMap.contains("relativeTime")) {
      parseRelativeTime(groupMap.get("relativeTime"), createdAt)
    } else {
      parseAbsoluteTime(groupMap.get("time"), groupMap.get("when"))
    }

    firstTime match {
      case Some(time) => ReminderParsing.Success(what.get, time, repeat)
      case None =>
        Logger.error("Failed to parse time {}", groupMap("time"))
        ReminderParsing.Failure
    }
  }


  /**
   * The main method that converts a tweet into the
   * logical contents of a reminder
   * Return None if the parsing fails or if the tweet
   * doesn't match the structure of an acceptable reminder.
   * A creation time must be included to properly handle relative
   * reminder times ie "in 4 hours".
   * @param text The tweet content to match
   * @param time The time the reminder was created.
   * @return
   */
  def parseStatusTextWithoutValidation(text: String, time: DateTime) = {

    Logger.info("Checking text: {} at created time {}", text, time)

    parseStatusTextIntoReminderData(text) match {
      case Some(result) =>
        parseReminderDataIntoReminder(result, time)
      case None =>
        println("No matching patterns found")
        Failure
    }
  }


  def createReminderFromTextAndTime(text: String, time: DateTime) = {
    parseStatusTextWithoutValidation(text, time) match {
      case s: Success if s.firstTime.isAfter(time) => s
      case s: Success if s.firstTime.isBefore(time) => ReminderParsing.DateTooEarly
      case x => x
    }
  }


  def getRepeatFrequency(repeat: Option[String]): Frequency = {

    if (repeat.isEmpty) {
      return Repeat.Never
    }

    val freq = repeat.get

    val daily = """(?i).*day.*""".r
    val weekly = """(?i).*week.*""".r
    val monthly = """(?i).*month.*""".r
    val hourly = """(?i).*hour.*""".r

    freq match {
      case daily() => Repeat.Daily
      case weekly() => Repeat.Weekly
      case monthly() => Repeat.Monthly
      case hourly() => Repeat.EveryHour
      case _ =>
        try {
          Repeat.withName(freq)
        } catch {
          case e: InvocationTargetException => Repeat.Never
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


  def parseAbsoluteTime(time: Option[String], date: Option[String]) : Option[DateTime] = {
    if (time.isDefined && date.isDefined) {
      Some(parseTimeAndDate(time.get, date.get))
    }
    else if (time.isDefined) {
      Some(parseTimeTodayOrTomorrow(time.get))
    }
    else if (date.isDefined) {
      Some(parseDate(date.get).toDateTimeAtStartOfDay.withTime(12,0,0,0))
    }
    else {
      None
    }
  }


  def parseRelativeTime(relativeTime: Option[String], createdAt: DateTime): Option[DateTime] = {
    relativeTime match {
      case Some(durationString) => Some(createdAt.plus(createDuration(durationString)))
      case None => None
    }
  }

  // Potential useful libraries:
  // https://github.com/joestelmach/natty
  // https://github.com/collegeman/stringtotime


  /**
   * Use the nice natty library to parse english strings into
   * dates.  Unfortunately, it doesn't easily expose relative
   * dates, only absolute ones.  It interprets relative ones
   * as relative to now.  So, we subtract away relative ones
   * to get the duration.  Somewhat hacky.
   * @param duration
   * @return
   */
  def getDateTimeFromDuration(duration: String): Option[Duration] = {
    val parser = new com.joestelmach.natty.Parser()
    val groups = parser.parse(duration)

    if (groups.size == 0) {
      None
    } else {
      val dates = groups.get(0).getDates

      if (dates.size == 0) {
        None
      } else {
        val pointInFuture = new DateTime(dates.get(0))
        val duration = new Duration(DateTime.now, pointInFuture)
        Some(duration)
      }
    }
  }


  def createDuration(duration: String): Duration = {

    val matcher = new Regex("(?i).*(\\d+)\\s+(minute|hour|day|week|month).*",
      "amount", "duration")

    matcher.findFirstMatchIn(duration) match {
      case Some(group) =>
        convertRegexToGroupMap(group)
    }



    Logger.error("Relative time not yet supported: {}", duration)
    // TODO: Fill this out
    new Duration
  }


  def parseRelativeTime(time: Option[String], when: Option[String]) : Option[DateTime] = {
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
      case e: Exception => DateTime.now().withZone(timeZone).toLocalTime
    }

    // Then, parse the date
    val date: LocalDate = parseDate(dateString)

    date.toDateTime(timeOfDay, timeZone)

  }


  def parseDate(dateString: String): LocalDate = {
    dateString.toUpperCase match {
      case "TODAY" => LocalDate.now()
      case "TOMORROW" => LocalDate.now().plusDays(1)
      case "MONDAY" => getNextDayOfWeek(DateTimeConstants.MONDAY)
      case "TUESDAY" => getNextDayOfWeek(DateTimeConstants.TUESDAY)
      case "WEDNESDAY" => getNextDayOfWeek(DateTimeConstants.WEDNESDAY)
      case "THURSDAY" => getNextDayOfWeek(DateTimeConstants.THURSDAY)
      case "FRIDAY" => getNextDayOfWeek(DateTimeConstants.FRIDAY)
      case "SATURDAY" => getNextDayOfWeek(DateTimeConstants.SATURDAY)
      case "SUNDAY" => getNextDayOfWeek(DateTimeConstants.SUNDAY)
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

    // For now, interpret all time zones as LA
    val timeAtToday = setTime(DateTime.now().withZone(timeZone), time)

    if (timeAtToday.isAfter(DateTime.now())) {
      timeAtToday
    } else {
      setTime(DateTime.now().plusDays(1).withZone(timeZone), time)
    }
  }

  def setTime(dateTime: DateTime , time: LocalTime) = {
    dateTime.withTime(time.getHourOfDay, time.getMinuteOfHour, time.getSecondOfMinute, time.getMillisOfSecond)
  }

  def parseTwelveHour(time: String): Option[LocalTime] = {

    Logger.info("Parsing Time: {}", time)

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("HH:mm")))
    } catch { case e: java.lang.IllegalArgumentException => }

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("hh:mm aa")))
    } catch { case e: java.lang.IllegalArgumentException => }

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("hh:mmaa")))
    } catch { case e: java.lang.IllegalArgumentException => }

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("hh")))
    } catch { case e: java.lang.IllegalArgumentException => }

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("HH")))
    } catch { case e: java.lang.IllegalArgumentException => }

    None
  }
}

