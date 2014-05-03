package models

import play.Logger

import org.joda.time.{LocalTime, DateTime, Interval, LocalDateTime}
import app.MyPostgresDriver.simple._

import app.MyPostgresDriver.simple.Tag
import helpers.Database.getDatabase
import scala.util.matching.Regex
import models.Tweets.TweetHelpers
import java.text.SimpleDateFormat
import java.util.Date
import org.joda.time.format.DateTimeFormat


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
case class Reminder(id: Option[Long], userId: Long, createdAt: DateTime,
                    repeat: String, firstTime: DateTime,
                    what: String, tweetId: Long) {

  def getScheduledReminders: List[ScheduledReminder] = {
    getDatabase().withSession{implicit session: Session =>
      return (for { b <- ScheduledReminders.scheduledReminders if b.reminderId is this.id} yield b).list
    }
  }
}


class Reminders(tag: Tag) extends Table[Reminder](tag, "reminders") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("userid", O.NotNull)
  def createdAt = column[DateTime]("createdat")
  def repeat = column[String]("repeat")
  def firstTime = column[DateTime]("firsttime")
  def what = column[String]("what")
  def tweetId = column[Long]("tweetId")

  def * = (id.?,  userId, createdAt, repeat, firstTime, what, tweetId) <> (Reminder.tupled, Reminder.unapply _)

}

object Reminders {

  val reminders = TableQuery[Reminders]

  def findById(id: Long)(implicit s: Session): Option[Reminder] = {
    reminders.where(_.id === id).firstOption
  }

  def insert(reminder: Reminder)(implicit s: Session) {
    reminders.insert(reminder)
  }

  def insertAndGet(reminder: Reminder)(implicit s: Session): Reminder = {
    val userId = (reminders returning reminders.map(_.id)) += reminder
    reminder.copy(id = Some(userId))
  }

  def update(id: Long, reminder: Reminder)(implicit s: Session) {
    val reminderToUpdate: Reminder = reminder.copy(Some(id))
    reminders.where(_.id === id).update(reminderToUpdate)
  }

  def delete(id: Long)(implicit s: Session) {
    reminders.where(_.id === id).delete
  }

  def createAndSaveIfReminder(user: models.User, tweet: models.Tweet, parsed: ReminderParsing.Parsed) (implicit s: Session) {
    parsed match {
      case ReminderParsing.Success(repeat, time, what) =>
        val reminder = Reminders.createFromTweet(user, tweet, ReminderParsing.Success(repeat, time, what))
        Tweets.insert(tweet)
        Reminders.insert(reminder)
        ScheduledReminders.scheduleFirstReminder(reminder)
        Logger.info("Found reminder in tweet: %s %s %s".format(what, time, repeat))
      case _ =>
        Logger.info("Did not find reminder in tweet")
    }
  }

  def createRemindersFromUserTwitterStatuses(user: models.User, statuses: Iterable[twitter4j.Status]) (implicit s: Session) {
    for (status <- statuses) {
      val tweet = TweetHelpers.fromStatus(user, status)
      val parsed =  ReminderParsing.parseStatusText(status.getText)
      createAndSaveIfReminder(user, tweet, parsed)
    }
  }

  def createFromTweet(user: User, tweet: Tweet, parsed: ReminderParsing.Success) =  {
    Reminder(None, user.id.get, DateTime.now(),parsed.repeat, parsed.firstTime, parsed.what, tweet.id.get)
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
  //def createReminder(status: twitter4j.Status): Option[Reminder] = {
  def createReminder(tweet: models.Tweet): Option[Reminder] = {

    val parsed = ReminderParsing.parseStatusText(tweet.getStatus.getText)

    parsed match {
      case ReminderParsing.Success(repeat, firstTime, what) =>
        Some(Reminder(None, tweet.userId, DateTime.now(), repeat, firstTime, what, tweet.id.get))
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
  case class Success(repeat: String, firstTime: DateTime, what: String) extends Parsed {
    override def isParsedSuccessfully = true
  }
  case object Failure extends Parsed
  case object DateTooEarly extends Parsed
  case object InvalidDate extends Parsed

  // TODO: Replace the repeat value with this
  sealed abstract class Repeat
  case object Never extends Repeat
  case class Every(interval: Interval) extends Repeat


  val pattern = new Regex("(?iu)@RemindTweets Remind Me (to)? (.+?) (on (.+?))? (at (.+?))? (every (.+?))?$",
    "to", "what", "on", "when", "at", "time", "every", "repeat")


  def getResultOfReminderRegex(text: String): Option[Regex.Match] = {
    pattern.findFirstMatchIn(text)
  }

  def convertRegexToGroupMap(matched: Regex.Match): Map[String,String] = {
    (for ((name, group) <- matched.groupNames zip matched.subgroups) yield name -> group).toMap
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

    val result = getResultOfReminderRegex(text) //pattern.findFirstMatchIn(text)
    if(result.isEmpty) {
      Logger.info("Didn't match pattern: {}", text)
      return ReminderParsing.Failure
    }

    Logger.info("Found match pattern: {}", text)

    val groupMap = convertRegexToGroupMap(result.get)

    //val groups = result.get
    //val what = groups.group("what")
    //val repeat = groups.group("repeat").replace(" ", "")
    //val parsedTime = parseReminderTime(groups.group("when"))

    val what = groupMap("what")
    val repeat = groupMap("repeat")

    val parsedTime = parseReminderTime(groupMap("time"))

    parsedTime match {
      case None =>
        Logger.error("Failed to parse time {}", groupMap("time"))
        ReminderParsing.Failure
      case Some(time) =>
        if (time.isAfter(DateTime.now())) {
          ReminderParsing.Success(repeat, time, what)
        } else {
          ReminderParsing.DateTooEarly
        }
    }
  }

  /**
   * Takes a string and returns the
   * @param timeString
   * @return
   */
  def parseReminderTime(timeString: String) : Option[DateTime] = {
    try {
      return Some(DateTime.parse(timeString))
    } catch { case e: Exception =>  }

    val twelveHour = parseTwelveHour(timeString)

    if (twelveHour.isDefined) {
      Some(DateTime.now().withTime(twelveHour.get.getHourOfDay, twelveHour.get.getMinuteOfHour,
        twelveHour.get.getSecondOfMinute,twelveHour.get.getMillisOfSecond))
    }

    None

  }

  def parseTwelveHour(time: String): Option[LocalTime] = {
    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("hh:mm aa")))
    } catch { case e: java.lang.IllegalArgumentException => }

    try {
      return Some(LocalTime.parse(time, DateTimeFormat.forPattern("hh:mmaa")))
    } catch { case e: java.lang.IllegalArgumentException => }

    None
  }
}

