package helpers


import helpers.ReminderParsing.DayOfWeek.DayOfWeek
import helpers.ReminderParsing.RelativeDate.RelativeDate
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
  case class DateTooEarly(firstTime: DateTime) extends Parsed
  case object InvalidDate extends Parsed
  case object NoWhat extends Parsed

  def convertRegexToGroupMap(matched: Regex.Match): Map[String,String] = {
    (for ((name, group) <- matched.groupNames zip matched.subgroups if group != null) yield name -> group).toMap
  }

  // Relative Time Non Recurring
  // Example: Remind me (to) (WHAT) (in) (4 hours).
  val patternA = new Regex("(?i)@RemindTweets\\s+Remind\\s+Me\\s*(to)?\\s+(.+?)\\s+(in)\\s+(.+?)\\.?$",
    "to", "what", "in", "relativeTime")

  // Absolute Time With recurring
  // Example: "Remind me (to) (WHAT) (on) (Tuesday) (at) (6:00pm) (every) (week)."
  val patternB = new Regex("(?i)@RemindTweets\\s+Remind\\s+Me\\s*(to)?\\s+(.+?)\\s+(every)\\s+(.+?)\\s+(at)\\s+(.+?)\\s*\\.?$",
    "to", "what", "every", "repeat", "at", "time")

  // Absolute Time With recurring
  // Example: "Remind me (to) (WHAT) (on) (Tuesday) (at) (6:00pm) (every) (week)."
  val patternC = new Regex("(?i)@RemindTweets\\s+Remind\\s+Me\\s*(to)?\\s+(.+?)\\s+(on)\\s+(.+?)\\s*(at)\\s+(.+?)\\s*(every)\\s+(.+?)\\.?$",
    "to", "what", "on", "when", "at", "time", "every", "repeat")

  // Absolute Time With recurring
  // Example: "Remind me to (WHAT) (on) (Tuesday) (at) (6:00pm)."
  val patternD = new Regex("(?i)@RemindTweets\\s+Remind\\s+Me\\s*(to)?\\s+(.+?)\\s+(on)\\s+(.+?)\\s+(at)\\s+(.+?)\\s*\\.?$",
    "to", "what", "on", "when", "at", "time")

  // Absolute Time With recurring
  // Example: "Remind me to WHAT on Tuesday at 6:00pm."
  val patternE = new Regex("(?i)@RemindTweets\\s+Remind\\s+Me\\s*(to)?\\s+(.+)\\s+(at)\\s+(.+?)\\s*\\.?$",
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

    val patterns = List(patternA, patternB, patternC, patternD, patternE)

    // Go most specific to least specific
    for (pattern <- patterns) {
      pattern.findFirstMatchIn(text) match {
        case Some(group) =>
          Logger.debug("Mention {} matches pattern: {}", text, pattern)
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

    val firstTime = determineFirstTime(groupMap, createdAt)

    firstTime match {
      case Some(time) => ReminderParsing.Success(what.get, time, repeat)
      case None =>
        Logger.error("Failed to parse time {}", groupMap("time"))
        ReminderParsing.Failure
    }
  }


  def determineFirstTime(groupMap: Map[String,String], createdAt: DateTime): Option[DateTime] = {

    // "In 4 hours"
    if (groupMap.contains("relativeTime")) {
      return parseRelativeTime(groupMap.get("relativeTime"), createdAt)
    }

    // "On Tuesday at 6:00PM"
    if (groupMap.contains("time") && groupMap.contains("when")) {
      return parseAbsoluteTime(groupMap.get("time"), groupMap.get("when"))
    }

    // "every Tuesday"
    if (groupMap.contains("time") && isDayOfWeek(groupMap.get("repeat"))) {
      return parseAbsoluteTime(groupMap.get("time"), groupMap.get("repeat"))
    }

    // "at 5:00PM"
    if (groupMap.contains("time")) {
      return parseNextTime(groupMap.get("time"))
    }

    //parseReminderTime(groupMap.get("time").get)

    None
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
      case s: Success if s.firstTime.isBefore(time) => ReminderParsing.DateTooEarly(s.firstTime)
      case x => x
    }
  }


  def getRepeatFrequency(repeat: Option[String]): Frequency = {

    if (repeat.isEmpty) {
      return Repeat.Never
    }

    val freq = repeat.get


    if (daysOfWeek.contains(freq.toUpperCase)) {
      return Repeat.Weekly
    }

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
      Some(parseNextDate(date.get).toDateTimeAtStartOfDay.withTime(12,0,0,0))
    }
    else {
      None
    }
  }

  /*
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
*/

  def parseRelativeTime(relativeTime: Option[String], createdAt: DateTime): Option[DateTime] = {
    relativeTime match {
      case Some(durationString) => getDateTimeFromDuration(durationString)
      case None => None
    }
  }


  def parseNextTime(relativeTime: Option[String]): Option[DateTime] = {
    if (!relativeTime.isDefined) {
      None
    } else {

      val time = getDateTimeFromDuration(relativeTime.get)

      if (!time.isDefined) {
        None
      } else {

        if (time.get.isBefore(DateTime.now())) {
          Some(time.get.plusDays(1))
        } else {
          Some(time.get)
        }
      }
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
  def getDateTimeFromDuration(duration: String): Option[DateTime] = {
    val parser = new Parser()
    val groups = parser.parse(duration)

    if (groups.size == 0) {
      None
    } else {

      val dates = groups.get(0).getDates

      if (dates.size == 0) {
        None
      } else {
        Some(new DateTime(dates.get(dates.size()-1)))
      }
    }
  }


  /*
    def createDurationBad(duration: String): Duration = {

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
  */



  def parseTimeAndDate(timeString: String, dateString: String): DateTime = {

    // First, parse the time of day string
    val timeOfDay: LocalTime = try {
      parseTwelveHour(timeString).get
    } catch {
      case e: Exception =>
        getDateTimeFromDuration(timeString) match {
          case Some(dateTime) => dateTime.toLocalTime
          case None =>
            Logger.error("Falling back to current time in 'parseTimeAndDate'")
            DateTime.now().withZone(timeZone).toLocalTime
        }
    }


    // Then, parse the date
    val thing = parseDate(dateString)
    thing match {
      case x: RelativeDate.RelativeDate => parseRelativeDate(x).toDateTime(timeOfDay, timeZone)
      case x: DayOfWeek => getNextDayOfWeekAtTime(x, timeOfDay)
      case _ => new LocalDate().toDateTime(timeOfDay, timeZone)
    }
  }


  def parseNextDate(dateString: String): LocalDate = {
    parseDate(dateString) match {
      case x: RelativeDate.RelativeDate => parseRelativeDate(x)
      case x: DayOfWeek => getNextDayOfWeek(x.id)
      case _ => new LocalDate()
    }
  }


    def parseRelativeDate(date: RelativeDate.RelativeDate): LocalDate = {
      date match {
        case RelativeDate.Today => LocalDate.now.plusDays(0)
        case RelativeDate.Tomorrow => LocalDate.now.plusDays(1)
      }
    }


  def getNextDayOfWeekAtTime(day: DayOfWeek, time: LocalTime): DateTime = {
    getNextDayOfWeek(day.id).toDateTime(time, timeZone) match {
      case x if x.isBefore(DateTime.now) => x.plusWeeks(1)
      case x => x
    }
  }

  object RelativeDate {
    sealed abstract trait RelativeDate
    case object Today extends RelativeDate
    case object Tomorrow extends RelativeDate
  }

  /*
    object RelativeDate extends Enumeration {
      type RelativeDate = RelativeDate.Value
      val Today, Tomorrow = RelativeDate.Value
    }
    */

    object DayOfWeek extends Enumeration {
      type DayOfWeek = Value
      val Monday = DayOfWeek.Value(DateTimeConstants.MONDAY, "Monday")
      val Tuesday = DayOfWeek.Value(DateTimeConstants.TUESDAY, "Tuesday")
      val Wednesday = DayOfWeek.Value(DateTimeConstants.WEDNESDAY, "Wednesday")
      val Thursday = DayOfWeek.Value(DateTimeConstants.THURSDAY, "Thursday")
      val Friday = DayOfWeek.Value(DateTimeConstants.FRIDAY, "Friday")
      val Saturday = DayOfWeek.Value(DateTimeConstants.SATURDAY, "Saturday")
      val Sunday = DayOfWeek.Value(DateTimeConstants.SUNDAY, "Sunday")
    }


    def parseDate(dateString: String) = {

      dateString.toUpperCase match {
        case "TODAY" => RelativeDate.Today //LocalDate.now()
        case "TOMORROW" => RelativeDate.Tomorrow //LocalDate.now().plusDays(1)

        //case "MONDAY" => getNextDayOfWeek(DateTimeConstants.MONDAY)

        case "MONDAY" => DayOfWeek.Monday //DateTimeConstants.MONDAY
        case "TUESDAY" => DayOfWeek.Tuesday //DateTimeConstants.TUESDAY
        case "WEDNESDAY" => DayOfWeek.Wednesday //DateTimeConstants.WEDNESDAY
        case "THURSDAY" => DayOfWeek.Thursday //   DateTimeConstants.THURSDAY
        case "FRIDAY" => DayOfWeek.Friday //DateTimeConstants.FRIDAY
        case "SATURDAY" => DayOfWeek.Saturday //DateTimeConstants.SATURDAY
        case "SUNDAY" => DayOfWeek.Sunday //DateTimeConstants.SUNDAY

        case _ => None //LocalDate.now()
      }
    }


    def isDayOfWeek(str: Option[String]): Boolean= {
      str match {
        case Some(day) => daysOfWeek.contains(day.toUpperCase)
        case None => false
      }
    }


    val daysOfWeek = Set("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")

    def getNextDayOfWeek(dayOfWeek: Int): LocalDate = {
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

