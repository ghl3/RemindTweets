package models

import org.joda.time.DateTime
import app.MyPostgresDriver.simple._

import app.MyPostgresDriver.simple.Tag


// Based on:
// http://slick.typesafe.com/doc/2.0.0-M3/lifted-embedding.html#inserting

/**
 * A SchuledReminder is an object representing a single reminder
 * to be sent out to the user at a specific time.  There sould be
 * a one-to-one correspondence bewteen ScheduledReminders and
 * reminder tweets that are sent out.
 * A Reminder may result in the creation of many ScheduledReminders
 * (possibly infinitely many if the original reminder repeats)
 * @param id
 * @param reminderId
 * @param time
 * @param executed
 */
case class ScheduledReminder(id: Option[Long], reminderId: Long, userId: Long, time: DateTime,
                             executed: Boolean, cancelled: Boolean, inProgress: Boolean, failed: Boolean) {

  def this(id: Option[Long], reminderId: Long, userId: Long, time: DateTime) = this(id, reminderId, userId, time, false, false, false, false)

  def getReminder(implicit s: Session): Option[Reminder] = {
    Reminders.findById(reminderId)
  }

  def setInProgress(progress: Boolean=true): ScheduledReminder = {
    this.copy(inProgress=progress, executed=false, cancelled=false, failed=false)
  }

  def setExecuted(executed: Boolean=true): ScheduledReminder = {
    this.copy(inProgress=false, executed=executed, cancelled=false, failed=false)
  }

  def setCancelled(cancelled: Boolean=true): ScheduledReminder = {
    this.copy(inProgress=false, executed=false, cancelled=cancelled, failed=false)
  }

  def setFailed(failed: Boolean=true): ScheduledReminder = {
    this.copy(inProgress=false, executed=false, cancelled=false, failed=failed)
  }
}


class ScheduledReminders(tag: Tag) extends Table[ScheduledReminder](tag, "scheduledreminders") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def reminderId = column[Long]("reminder_id", O.NotNull)
  def userId = column[Long]("user_id", O.NotNull)
  def time = column[DateTime]("time")
  def executed = column[Boolean]("executed")
  def cancelled = column[Boolean]("cancelled")
  def inProgress = column[Boolean]("in_progress")
  def failed = column[Boolean]("failed")

  def * = (id.?, reminderId, userId, time, executed, cancelled, inProgress, failed) <> (ScheduledReminder.tupled, ScheduledReminder.unapply)

  // Create a foreign key relationship on reminders
  def reminder = foreignKey("SCHEDULEDREMINDER_REMINDER_FK", reminderId, Reminders.reminders)(_.id)

  // Create a foreign key relationship on reminders
  def user = foreignKey("SCHEDULEDREMINDER_USER_FK", userId, Users.users)(_.id)
}


object ScheduledReminders {

  val scheduledReminders = TableQuery[ScheduledReminders]

  def findById(id: Long)(implicit s: Session): Option[ScheduledReminder] = {
    scheduledReminders.where(_.id === id).firstOption
  }

  def insert(scheduledReminder: ScheduledReminder)(implicit s: Session) = {
    scheduledReminders.insert(scheduledReminder)
  }

  def insertAndGet(scheduledReminder: ScheduledReminder)(implicit s: Session): ScheduledReminder = {
    val userId = (scheduledReminders returning scheduledReminders.map(_.id)) += scheduledReminder
    scheduledReminder.copy(id = Some(userId))
  }

  def update(id: Long, scheduledReminder: ScheduledReminder)(implicit s: Session) = {
    val reminderToUpdate: ScheduledReminder = scheduledReminder.copy(Some(id))
    scheduledReminders.where(_.id === id).update(reminderToUpdate)
  }

  def update(scheduledReminder: ScheduledReminder)(implicit s: Session) = {
    scheduledReminders.where(_.id === scheduledReminder.id.get).update(scheduledReminder)
  }

  def delete(id: Long)(implicit s: Session) = {
    scheduledReminders.where(_.id === id).delete
  }

  def scheduleFirstReminder(reminder: Reminder)(implicit s: Session): ScheduledReminder = {
    val scheduledReminder = new ScheduledReminder(None, reminder.id.get, reminder.userId, reminder.firstTime)
    insertAndGet(scheduledReminder)
  }

  def getRemindersToSchedule(minDateTime: DateTime, maxDateTime: DateTime)(implicit s: Session) = {
    scheduledReminders.filter(_.executed===false)
      .filter(_.cancelled===false)
      .filter(_.inProgress===false)
      .filter(_.failed===false)
      .filter(_.time > minDateTime)
      .filter(_.time < maxDateTime).list
  }

}