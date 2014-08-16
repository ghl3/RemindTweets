package actors

import akka.actor._
import akka.routing.RoundRobinRouter
import helpers.TwitterApi
import models.ScheduledReminders
import play.Logger

import play.libs.Akka
import java.util.concurrent.TimeUnit

import play.api.Play.current
import org.joda.time

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global


object ReminderScheduler {

  def beginScheduler(interval: concurrent.duration.FiniteDuration, nTweeters: Integer) {

    // Create an Akka system
    val system = ActorSystem("ReminderScheduler")

    val master = system.actorOf(Props(new ReminderScheduler(nTweeters)), name="masterScheduler")

    val minOffset = org.joda.time.Duration.millis(interval.toMillis / 2)
    val maxOffset = org.joda.time.Duration.millis(interval.toMillis * 2)

    // Schedule a new batch to be run every 30 seconds
    // The new batch is obtained from reminders scheduled for
    // 30 seconds into the future until 60 seconds into the future
    Akka.system().scheduler.schedule(
      concurrent.duration.Duration.create(0, TimeUnit.MILLISECONDS),
      interval,
      master,
      Schedule(minOffset, maxOffset))
  }
}

case class Schedule(minInterval: time.Duration, maxInterval: time.Duration)
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
      play.api.db.slick.DB.withSession { implicit session =>

          val minDateTime = time.DateTime.now().plus(min)
          val maxDateTime = time.DateTime.now().plus(max)
          Logger.debug("Received Schedule {} {}", minDateTime, maxDateTime)

          val scheduledReminders = ScheduledReminders.getRemindersToSchedule(minDateTime, maxDateTime)

          Logger.debug("Found %s scheduled reminders to handle now".format(scheduledReminders.size))

          for {scheduledReminder <- scheduledReminders
               reminder <- scheduledReminder.getReminder
               user <- reminder.getUser} {

            val duration = new time.Duration(time.DateTime.now(), scheduledReminder.time)
            val concurrentDuration = concurrent.duration.Duration.create(duration.getMillis, TimeUnit.MILLISECONDS)

            Logger.info("Sending message to {} at {} ({})", user.screenName, scheduledReminder.time, duration.toString)
            context.system.scheduler.scheduleOnce(concurrentDuration) {
              workerRouter ! TweetRequest(scheduledReminder.id.get, user.screenName, reminder.what)
            }

            // Mark it as in progress so it doesn't get double scheduled
            ScheduledReminders.update(scheduledReminder.setInProgress(progress=true))
          }
      }

    case ReminderSuccess(scheduledReminderId) =>
      play.api.db.slick.DB.withSession { implicit session =>
          val scheduledReminder = ScheduledReminders.findById(scheduledReminderId)
          if (scheduledReminder.isDefined) {
            ScheduledReminders.update(scheduledReminder.get.setExecuted())
          } else {
            Logger.error("Could not find scheduled reminder with id: %s" format scheduledReminderId)
          }
      }

    case ReminderFailure(scheduledReminderId) =>
      play.api.db.slick.DB.withSession { implicit session =>
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
        sender ! ReminderSuccess(scheduledReminderId)

        TwitterApi.sendTweetToUser(screenName, content)

        val status: String = "%s %s".format(screenName, content)
        Logger.info("Sent tweet '{}'", status)
      } catch {
        case e: Exception => sender ! ReminderFailure(scheduledReminderId)
      }
  }
}
