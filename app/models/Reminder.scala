package models

import org.joda.time.LocalDateTime
import app.MyPostgresDriver.simple._
import play.api.Play.current
import play.Logger
import scala.util.matching.Regex

import helpers.Database.getDatabase

import scala.slick.lifted._


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
                    request: String, content: String) /*{


  def getScheduledReminders: List[ScheduledReminder] = {
    getDatabase().withSession{implicit session: Session =>
      return (for { b <- ScheduledReminders if b.reminderId is this.id} yield b).list
    }
  }


}*/

object Reminder {

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

  /**
   * Is the supplied twitter status a request for a reminder
   * @param statusText
   * @return
   */
  def isReminder(status: twitter4j.Status): Boolean = {
    val parsed = parseStatusText(status.getText)
    return parsed != None
  }

  def createReminder(status: twitter4j.Status): Option[Reminder] = {

    val successfulParse = parseStatusText(status.getText)
    if (successfulParse.isEmpty) {
      return None
    }

    val parsed = successfulParse.get

    return Option(Reminder(None, status.getUser.getId, LocalDateTime.now(),
    parsed.repeat, parsed.firstTime, parsed.request, status.getText))

  }


}


// Definition of the COFFEES table
class Reminders(tag: Tag) extends Table[Reminder](tag, "reminders") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("userid", O.NotNull)
  def createdAt = column[LocalDateTime]("createdat")
  def repeat = column[String]("repeat")
  def firstTime = column[LocalDateTime]("firsttime")
  def request = column[String]("request")
  def content = column[String]("content")

  def * : ColumnBase[Reminder] = (id.?,  userId, createdAt, repeat, firstTime, request, content) <> (Reminder.tupled, Reminder.unapply)

  // These are both necessary for auto increment to work with psql
  def autoInc =  userId ~ createdAt ~ repeat ~ firstTime ~ request ~ content returning id

  def addToTable(reminder: Reminder): Long = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      Logger.info("Adding reminder to table: {}", reminder)
      val id = Reminders.autoInc.insert(reminder.userId, reminder.createdAt, reminder.repeat, reminder.firstTime, reminder.request, reminder.content)
      return id
      //return fetch(id).get
    }
  }

  def update(reminder: Reminder): Reminder = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = Reminders.insert(reminder)
      return fetch(id).get
    }
  }

  def fetch(id: Long): Option[Reminder] = {
    Logger.info("Looking for reminder with id: {}", id.toString)
    play.api.db.slick.DB.withSession{implicit session: Session =>
      (for { b <- Reminders if b.id is id} yield b).firstOption
    }
  }


}


