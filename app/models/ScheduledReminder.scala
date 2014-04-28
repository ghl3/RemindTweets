package models

import org.joda.time.LocalDateTime
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
case class ScheduledReminder(id: Option[Long], reminderId: Long, userId: Long,
                             time: LocalDateTime, executed: Boolean, cancelled: Boolean)


// Definition of the COFFEES table
class ScheduledReminders(tag: Tag) extends Table[ScheduledReminder](tag, "scheduledreminders") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def reminderId = column[Long]("reminderid", O.NotNull)
  def userId = column[Long]("userid", O.NotNull)
  def time = column[LocalDateTime]("time")
  def executed = column[Boolean]("executed")
  def cancelled = column[Boolean]("cancelled")

  def * = (id.?, reminderId, userId, time, executed, cancelled) <> (ScheduledReminder.tupled, ScheduledReminder.unapply)
}

object ScheduledReminders {
  val scheduledReminders = TableQuery[ScheduledReminders]


  def findById(id: Long)(implicit s: Session): Option[ScheduledReminder] = {
    scheduledReminders.where(_.id === id).firstOption
  }

  def insert(scheduledReminder: ScheduledReminder)(implicit s: Session) {
    scheduledReminders.insert(scheduledReminder)
  }

  def insertAndGet(scheduledReminder: ScheduledReminder)(implicit s: Session): ScheduledReminder = {
    val userId = (scheduledReminders returning scheduledReminders.map(_.id)) += scheduledReminder
    return scheduledReminder.copy(id = Some(userId))
  }

  def update(id: Long, scheduledReminder: ScheduledReminder)(implicit s: Session) {
    val reminderToUpdate: ScheduledReminder = scheduledReminder.copy(Some(id))
    scheduledReminders.where(_.id === id).update(reminderToUpdate)
  }

  def delete(id: Long)(implicit s: Session) {
    scheduledReminders.where(_.id === id).delete
  }

}