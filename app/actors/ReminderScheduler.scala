package actors

import akka.actor._
import akka.routing.RoundRobinRouter
import models.ScheduledReminders
import play.Logger

import play.libs.Akka
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

import play.api.Play.current
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
 * Manage the scheduling of reminders
 * and the handling of their success or failure
 */
class ReminderScheduler(nTweeters: Integer) extends Actor {

  val workerRouter = context.actorOf(
    Props[TweetSender].withRouter(RoundRobinRouter(nTweeters)), name = "reminderScheduler")


  override def receive: Receive = {

    case Schedule(nRemindersMax) =>
      play.api.db.slick.DB.withSession {
        implicit session =>

          val minDateTime = DateTime.now().plusSeconds(30)
          val maxDateTime = DateTime.now().plusSeconds(60)

          val scheduledReminders = ScheduledReminders.getRemindersToSchedule(minDateTime, maxDateTime)

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


case class Schedule(nRemindersMax: Long)

object ReminderScheduler {

  def calculate(nTweeters: Integer) {

    // Create an Akka system
    val system = ActorSystem("ReminderScheduler")

    val master = system.actorOf(Props(new ReminderScheduler(nTweeters)), name="master")

    // Create a new batch of reminders every 30 seconds
    // for a maximum of 100
    Akka.system().scheduler.schedule(
      Duration.create(0, TimeUnit.MILLISECONDS),
      Duration.create(30, TimeUnit.SECONDS),
        master, Schedule(1000))
  }
}