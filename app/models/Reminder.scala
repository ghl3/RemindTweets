package models

import play.Logger

import org.joda.time._
import app.MyPostgresDriver.simple._

import app.MyPostgresDriver.simple.Tag
import models.Tweets.TweetHelpers

import scala.Some
import models.Repeat.Frequency
import helpers.ReminderParsing
import helpers.TwitterApi.TwitterStatusAndJson


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

  def createRemindersFromUserTwitterStatuses(user: models.User, statuses: Iterable[TwitterStatusAndJson])(implicit s: Session): Iterable[Reminder] = {
    (for (status <- statuses) yield {
      val tweet = TweetHelpers.fromStatusAndJson(user, status.status, status.json)
      val parsed =  ReminderParsing.parseStatusText(status.status.getText)
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


  def getLatestReminderTwitterId()(implicit s: Session): Option[Long] = {

    val reminderTwitterIds = for {
      r <- Reminders.reminders
      t <- r.tweet
    } yield t.twitterId

    reminderTwitterIds.sortBy(_.desc).firstOption
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
