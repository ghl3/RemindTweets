package actors

import akka.actor._
import akka.routing.RoundRobinRouter
import models.ScheduledReminders
import play.Logger

import play.libs.Akka
import scala.concurrent.duration.{FiniteDuration, Duration}
import java.util.concurrent.TimeUnit

import play.api.Play.current
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global



object ReminderScheduler {

  def calculate(interval: FiniteDuration, nTweeters: Integer) {

    // Create an Akka system
    val system = ActorSystem("ReminderScheduler")

    val master = system.actorOf(Props(new ReminderScheduler(nTweeters)), name="master")

    // Schedule a new batch to be run every 30 seconds
    // The new batch is obtained from reminders scheduled for
    // 30 seconds into the future until 60 seconds into the future
    Akka.system().scheduler.schedule(
      Duration.create(0, TimeUnit.MILLISECONDS),
      interval,
      master,
      Schedule(Duration.create(30, TimeUnit.SECONDS), Duration.create(60, TimeUnit.SECONDS)))
  }
}

case class Schedule(minInterval: Duration, maxInterval: Duration)
case class ReminderSuccess(scheduledReminderId: Long)
case class ReminderFailure(scheduledReminderId: Long)

/**
 * Manage the scheduling of reminders
 * and the handling of their success or failure
 */
class ReminderScheduler(nTweeters: Integer) extends Actor {

  val workerRouter = context.actorOf(
    Props[TweetSender].withRouter(RoundRobinRouter(nTweeters)), name = "reminderScheduler")


  override def receive: Receive = {

    case Schedule(min, max) =>
      play.api.db.slick.DB.withSession {
        implicit session =>

          Logger.debug("Received Schedule {} {}", min, max)
          val minDateTime = DateTime.now().plus(min.length)
          val maxDateTime = DateTime.now().plus(max.length)

          val scheduledReminders = ScheduledReminders.getRemindersToSchedule(minDateTime, maxDateTime)

          Logger.debug("Found %s scheduled reminders to handle now".format(scheduledReminders.size))

          for {scheduledReminder <- scheduledReminders
               reminder <- scheduledReminder.getReminder
               user <- reminder.getUser} {
            workerRouter ! TweetRequest(scheduledReminder.id.get, user.screenName, reminder.what)
          }
      }

    case ReminderSuccess(scheduledReminderId) =>
      play.api.db.slick.DB.withSession { implicit session =>
          val scheduledReminder = ScheduledReminders.findById(scheduledReminderId)
          if (scheduledReminder.isDefined) {
            ScheduledReminders.insert(scheduledReminder.get.setExecuted())
          } else {
            Logger.error("Could not find scheduled reminder with id: %s" format scheduledReminderId)
          }
      }

    case ReminderFailure(scheduledReminderId) =>
      play.api.db.slick.DB.withSession {
        implicit session =>
          val scheduledReminder = ScheduledReminders.findById(scheduledReminderId)
          if (scheduledReminder.isDefined) {
            ScheduledReminders.insert(scheduledReminder.get.setFailed())
          } else {
            Logger.error("Could not find scheduled reminder with id: %s" format scheduledReminderId)
          }
      }
  }
}


case class TweetRequest(scheduledReminderId: Long, screenName: String, content: String)

class TweetSender extends Actor {

  override def receive = {
    case TweetRequest(scheduledReminderId, screenName, content) =>
      try {

        Logger.debug("Received Tweet to send: %s %s %s".format(scheduledReminderId, screenName, content))

        val status: String = "%s %s".format(screenName, content)
        Logger.info("Sending tweet '{}'", status)
        sender ! ReminderSuccess(scheduledReminderId)
      } catch {
        case e: Exception => sender ! ReminderFailure(scheduledReminderId)
      }
  }
}
