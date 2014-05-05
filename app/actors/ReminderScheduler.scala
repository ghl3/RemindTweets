package actors

import akka.actor._
import akka.routing.RoundRobinRouter
import models.ScheduledReminders
import play.Logger
import play.api.db.slick.Session

/**
 * Manage the scheduling of reminders
 * and the handling of their success or failure
 */
class ReminderScheduler(nTweeters: Integer, dbSession: Session) extends Actor {

  val workerRouter = context.actorOf(
    Props[TweetSender].withRouter(RoundRobinRouter(nTweeters)), name = "reminderScheduler")

  override def receive: Receive = {

    case ReminderSuccess(scheduledReminderId) =>
      implicit val rs = dbSession
      val scheduledReminder = ScheduledReminders.findById(scheduledReminderId)
      if (scheduledReminder.isDefined) {
        ScheduledReminders.insert(scheduledReminder.get.setExecuted())
      } else {
        Logger.error("Could not find scheduled reminder with id: %s" format scheduledReminderId)
      }

    case ReminderFailure(scheduledReminderId) =>
      implicit val rs = dbSession
      val scheduledReminder = ScheduledReminders.findById(scheduledReminderId)
      if (scheduledReminder.isDefined) {
        ScheduledReminders.insert(scheduledReminder.get.setFailed())
      } else {
        Logger.error("Could not find scheduled reminder with id: %s" format scheduledReminderId)
      }
  }
}
