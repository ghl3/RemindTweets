package helpers

import models.{ScheduledReminder, ScheduledReminders}
import play.Logger

import app.MyPostgresDriver.simple._

object ReminderIssuer {


  // Select all scheduled reminders and issue the tweets

  def issueReminders(implicit s: Session) = {
    for (scheduledReminder: ScheduledReminder <- ScheduledReminders.scheduledReminders
      .filter(_.cancelled === false).filter(_.executed === false)) {
      Logger.info("My reminder: {}", scheduledReminder)

      val reminder = scheduledReminder.getReminder
      Logger.info("Reminder: {}", reminder.get.what)
    }
  }
}
