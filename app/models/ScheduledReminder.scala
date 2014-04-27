package models

import org.joda.time.LocalDateTime
import app.MyPostgresDriver.simple._
//import scala.slick.lifted._

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

  /*
  def usersForInsert = scheduledReminders.map(u => (u.reminderId, u.userId, u.time, u.executed, u.cancelled).shaped <>
    ({ t => ScheduledReminder(None, t.reminderId, t.userId, t.time, t.executed, t.cancelled)},
    { (u: ScheduledReminder) => Some((u.reminderId, u.userId, u.time, u.executed, u.cancelled))}))
*/
}

/*
object ScheduledReminders{

  // These are both necessary for auto increment to work with psql
  def autoInc =  reminderId ~ userId ~time ~ executed ~ cancelled returning id

  def addToTable(reminder: ScheduledReminder): ScheduledReminder = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = ScheduledReminders.autoInc.insert(reminder.reminderId, reminder.userId, reminder.time, reminder.executed, reminder.cancelled)
      return fetch(id).get
    }
  }

  def update(reminder: ScheduledReminder): ScheduledReminder = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = ScheduledReminders.insert(reminder)
      return fetch(id).get
    }
  }

  def fetch(id: Long): Option[ScheduledReminder] = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      (for { b <- ScheduledReminders if b.id is id} yield b).firstOption
    }
  }

}

*/

