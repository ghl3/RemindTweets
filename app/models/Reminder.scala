package models

import play.Logger

import org.joda.time._
import app.MyPostgresDriver.simple._

import app.MyPostgresDriver.simple.Tag
import scala.util.matching.Regex
import models.Tweets.TweetHelpers

import org.joda.time.format.DateTimeFormat

import scala.Some
import models.Repeat.Frequency
import java.lang.reflect.InvocationTargetException


/**
 * A user-created request to be reminded
 * @param id The id in the database for this reminder
 * @param userId The id in the database for the user making the request
 * @param createdAt When the reminder was created
 * @param repeat The repeat strategy of the reminder
 * @param firstTime When the first reminder should be tweeted
 * @param what The text to be sent to the user at the remind time
 * @param tweetId The id of the tweet that initiated the reminder
 */
case class Reminder(id: Option[Long], userId: Long, twitterId: Long, createdAt: DateTime,
                    repeat: Frequency, firstTime: DateTime,
                    what: String, tweetId: Long) {


  def getScheduledReminders(implicit s: Session): List[ScheduledReminder] = {
    (for { b <- ScheduledReminders.scheduledReminders if b.reminderId is this.id} yield b).list
  }

  def getUser(implicit s: Session): Option[User] = {
    Users.findById(this.userId)
  }
}

object Repeat extends Enumeration {
  type Frequency = Value
  val Never, Daily, Weekly, Monthly, EveryHour = Value
}

class Reminders(tag: Tag) extends Table[Reminder](tag, "reminders") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("user_id", O.NotNull)
  def twitterId = column[Long]("twitter_id", O.NotNull)
  def createdAt = column[DateTime]("createdat")
  def repeat = column[Frequency]("repeat")
  def firstTime = column[DateTime]("firsttime")
  def what = column[String]("what")
  def tweetId = column[Long]("tweet_id")

  def * = (id.?,  userId, twitterId, createdAt, repeat, firstTime, what, tweetId) <> (Reminder.tupled, Reminder.unapply _)

  def uniqueTwitterId = index("UNIQUE_REMINDER_TWITTERID", twitterId, unique = true)
  def uniqueTweetId = index("UNIQUE_REMINDER_TWEETID", tweetId, unique = true)

  def user = foreignKey("TWEET_USER_FK", userId, Users.users)(_.id)

  def tweet = foreignKey("REMINDER_TWEET_FK", tweetId, Tweets.tweets)(_.id)

}


object Reminders {

  val reminders = TableQuery[Reminders]

  def findById(id: Long)(implicit s: Session): Option[Reminder] = {
    reminders.where(_.id === id).firstOption
  }

  def findByTwitterId(twitterId: Long) (implicit s: Session): Option[Reminder] = {
    reminders.where(_.twitterId === twitterId).firstOption
  }

  def insert(reminder: Reminder)(implicit s: Session) {
    reminders.insert(reminder)
  }

  def insertAndGet(reminder: Reminder)(implicit s: Session): Reminder = {
    val userId = (reminders returning reminders.map(_.id)) += reminder
    reminder.copy(id = Some(userId))
  }

  def update(id: Long, reminder: Reminder)(implicit s: Session) = {
    val reminderToUpdate: Reminder = reminder.copy(Some(id))
    reminders.where(_.id === id).update(reminderToUpdate)
  }

  def delete(id: Long)(implicit s: Session) = {
    reminders.where(_.id === id).delete
  }

  def createAndSaveIfReminder(user: models.User, tweet: models.Tweet, parsed: ReminderParsing.Parsed) (implicit s: Session): Option[Reminder] = {
    parsed match {
      case ReminderParsing.Success(what, time, repeat) =>

        Logger.info("Found reminder in tweet: %s %s %s".format(what, time, repeat))

        val savedTweet  = Tweets.insertIfUniqueTweetAndGet(tweet)
        val createdReminder = Reminders.createFromTweetIfUnique(user, savedTweet, ReminderParsing.Success(what, time, repeat))

        createdReminder match {
          case Some(reminder) =>
            Logger.info("Saving reminder into database and scheduling first reminder")
            val savedReminder = Reminders.insertAndGet(reminder)
            ScheduledReminders.scheduleFirstReminder(savedReminder)
            Some(reminder)
          case None =>
            Logger.info("Reminder already exists.  Not re-saving")
            None
        }
      case _ =>
        Logger.info("Did not find reminder in tweet")
        None
    }
  }

  def createRemindersFromUserTwitterStatuses(user: models.User, statuses: Iterable[twitter4j.Status])(implicit s: Session): Iterable[Reminder] = {
    (for (status <- statuses) yield {
      val tweet = TweetHelpers.fromStatus(user, status)
      val parsed =  ReminderParsing.parseStatusText(status.getText)
      createAndSaveIfReminder(user, tweet, parsed)
    }).flatten
  }

  /**
   * Create a new reminder for the given user from the
   * given tweet and the parsed result of the tweet.
   * If the tweet has already been used to create a reminder
   * that exist in the db, return None
   * @param user
   * @param tweet
   * @param parsed
   * @return
   */
  def createFromTweetIfUnique(user: User, tweet: Tweet, parsed: ReminderParsing.Success)(implicit s: Session): Option[Reminder] = {
    val existingReminder = findByTwitterId(tweet.twitterId)
    if (existingReminder.isDefined) {
      None
    } else {
      Some(createFromTweet(user, tweet, parsed)) //Reminder(None, user.id.get, tweet.twitterId, DateTime.now(), parsed.repeat, parsed.firstTime, parsed.what, tweet.id.get)_
    }
  }

  def createFromTweet(user: User, tweet: Tweet, parsed: ReminderParsing.Success) =  {
    Reminder(None, user.id.get, tweet.twitterId, DateTime.now(), parsed.repeat, parsed.firstTime, parsed.what, tweet.id.get)
  }

  /**
   * Is the supplied twitter status a request for a reminder
   * @param status
   * @return
   */
  def isReminder(status: twitter4j.Status): Boolean = {
    ReminderParsing.parseStatusText(status.getText) match {
      case ReminderParsing.Success(_,_,_) => true
      case _ => false
    }
  }


  /**
   * Attempt to create a reminder from a twitter status
   * @param tweet
   * @return
   */
  def createReminder(tweet: models.Tweet): Option[Reminder] = {

    val parsed = ReminderParsing.parseStatusText(tweet.getStatus.getText)

    parsed match {
      case ReminderParsing.Success(what, firstTime, repeat) =>
        Some(Reminder(None, tweet.userId, tweet.twitterId, DateTime.now(), repeat, firstTime, what, tweet.id.get))
      case _ => None
    }
  }
}

object ReminderHelper {

  def getRemidersFromTweets(tweets: Iterable[Tweet]) = {

    tweets.map(tweet => ReminderParsing.parseStatusText(tweet.getStatus.getText)).filter {
      case ReminderParsing.Success(_, _, _) => true
      case _ => false
    }
  }
}


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

