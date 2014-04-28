package models

import play.Logger

import org.joda.time.LocalDateTime
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
 * @param request The text to be sent to the user at the remind time
 * @param content The content of the user's request
 */
case class Reminder(id: Option[Long], userId: Long, createdAt: LocalDateTime,
                    repeat: String, firstTime: LocalDateTime,
                    request: String, content: String) {


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
    return reminder.copy(id = Some(userId))
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
    val parsed = ReminderHelper.parseStatusText(status.getText)
    return parsed != None
  }

  def createReminder(status: twitter4j.Status): Option[Reminder] = {

    val successfulParse = ReminderHelper.parseStatusText(status.getText)
    if (successfulParse.isEmpty) {
      return None
    }

    val parsed = successfulParse.get

    return Option(Reminder(None, status.getUser.getId, LocalDateTime.now(),
      parsed.repeat, parsed.firstTime, parsed.request, status.getText))
  }

}


object ReminderHelper {

  case class Parsed(repeat: String, firstTime: LocalDateTime, request: String)

  /**
   * The main method that converts a tweet into the
   * logical contents of a reminder
   * Return None if the parsing fails or if the tweet
   * doesn't match the structure of an acceptable reminder
   * @param text
   * @return
   */
  def parseStatusText(text: String): Option[Parsed] = {

    try {

      // TODO: Fuck this shit!
      //val pattern = new Regex("(?iu)@RemindTweets Remind Me (to)? (.+) at (.+) (every (.+))?", "to", "action", "time", "every", "repeat")
      //val pattern = new Regex("(?iu)@RemindTweets Remind Me (to)? (.+) on (.+) (every (.+))?")
      val pattern = new Regex("(?iu)@RemindTweets Remind Me (to)? (.+) at (.+)", "to", "request", "time")

      Logger.info("Checking text: {}", text)

      val result = pattern.findFirstMatchIn(text)
      if(result==None) {
        Logger.info("Didn't match pattern: {}", text)
        return None
      } else {
        Logger.info("Found match pattern: {}", text)
      }

      val groups = result.get

      val action = groups.group("request")
      val time = LocalDateTime.parse(groups.group("time"))

      return Option(Parsed("NEVER", time, action))

      /*
      //val matches: List[String] = pattern.findAllIn(text).map((item) => item.toString).toList

      Logger.info("Text: {} Matches: {}", text, matches)

      if (matches.size==2) {
        val request = matches(0)
        val firstTime = LocalDateTime.parse(matches(1))
        val repeat = "NEVER"
        return Option(Parsed(repeat, firstTime, request))
      }
      else if (matches.size==3) {
        val request = matches(0)
        val firstTime = LocalDateTime.parse(matches(1))
        val repeat = matches(2)
        return Option(Parsed(repeat, firstTime, request))
      }
      else {
        return None
      }
      */
    }
    catch {
      case e: Exception => {
        Logger.error("Failed to parse Text: {}", text, e)
        return None
      }
    }
    return None
  }

}

