package models

import play.Logger

import org.joda.time.{Interval, DateTime, LocalDateTime}
import app.MyPostgresDriver.simple._

import app.MyPostgresDriver.simple.Tag
import helpers.Database.getDatabase
import scala.util.matching.Regex


/**
 * A user-created request to be reminded
 * @param id The id in the database for this reminder
 * @param userId The id in the database for the user making the request
 * @param createdAt When the reminder was created
 * @param repeat The repeat strategy of the reminder
 * @param firstTime When the first reminder should be tweeted
 * @param what The text to be sent to the user at the remind time
 * @param request The content of the user's request
 */
case class Reminder(id: Option[Long], userId: Long, createdAt: LocalDateTime,
                    repeat: String, firstTime: LocalDateTime,
                    what: String, request: String) {


  def getScheduledReminders: List[ScheduledReminder] = {
    getDatabase().withSession{implicit session: Session =>
      return (for { b <- ScheduledReminders.scheduledReminders if b.reminderId is this.id} yield b).list
    }
  }
}


class Reminders(tag: Tag) extends Table[Reminder](tag, "reminders") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("userid", O.NotNull)
  def createdAt = column[LocalDateTime]("createdat")
  def repeat = column[String]("repeat")
  def firstTime = column[LocalDateTime]("firsttime")
  def request = column[String]("request")
  def content = column[String]("content")

  def * = (id.?,  userId, createdAt, repeat, firstTime, request, content) <> (Reminder.tupled, Reminder.unapply _)

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


  /**
   * Is the supplied twitter status a request for a reminder
   * @param status
   * @return
   */
  def isReminder(status: twitter4j.Status): Boolean = {
    ReminderHelper.parseStatusText(status.getText) match {
      case ReminderParsing.Success(_,_,_) => true
      case _ => false
    }
  }


  /**
   * Attempt to create a reminder from a twitter status
   * @param status
   * @return
   */
  def createReminder(status: twitter4j.Status): Option[Reminder] = {

    val parsed = ReminderHelper.parseStatusText(status.getText)

    parsed match {
      case ReminderParsing.Success(repeat, firstTime, what) =>
         Some(Reminder(None, status.getUser.getId, LocalDateTime.now(), repeat, firstTime, what, status.getText))
      case _ => None
    }
  }
}


object ReminderParsing {

  sealed abstract class Parsed
  case class Success(repeat: String, firstTime: LocalDateTime, what: String) extends Parsed
  case object Failure extends Parsed
  case object DateTooEarly extends Parsed
  case object InvalidDate extends Parsed

  // TODO: Replace the repeat value with this
  sealed abstract class Repeat
  case object Never extends Repeat
  case class Every(interval: Interval) extends Repeat

}


object ReminderHelper {

//  case class Parsed(repeat: String, firstTime: LocalDateTime, request: String)



  def getRemidersFromTweets(tweets: Iterable[Tweet]) = {

    tweets.map(tweet => parseStatusText(tweet.getStatus.getText)).filter {
      case ReminderParsing.Success(_,_,_) => true
      case _ => false
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

      val pattern = new Regex("(?iu)@RemindTweets Remind Me (to)? (.+) at (.+?) (every\\w?)?(.+)?",
        "to", "what", "when", "every", "repeat")

      Logger.info("Checking text: {}", text)

      val result = pattern.findFirstMatchIn(text)
      if(result==None) {
        Logger.info("Didn't match pattern: {}", text)
        return ReminderParsing.Failure
      } else {
        Logger.info("Found match pattern: {}", text)
      }

      val groups = result.get
      val what = groups.group("what")
      val repeat = groups.group("repeat").replace(" ", "")
      val parsedTime = parseReminderTime(groups.group("when"))

      parsedTime match {
        case None =>
          Logger.error("Failed to parse time {}", parsedTime)
          ReminderParsing.Failure
        case Some(time) =>
          if (time.isAfter(LocalDateTime.now())) {
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
  def parseReminderTime(timeString: String) : Option[LocalDateTime] = {
    try {
      Some(LocalDateTime.parse(timeString))
    } catch {
      case e: Exception =>
        Logger.error("Failed to parse time {}", timeString, e)
        None
    }
  }


}

