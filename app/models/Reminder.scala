package models

import org.joda.time.LocalDateTime
import app.MyPostgresDriver.simple._
import scala.slick.lifted._
import play.api.Play.current

case class Reminder(id: Option[Long], userId: Long, createdAt: LocalDateTime,
                    repeat: String, firstTime: LocalDateTime,
                    request: String, content: String)


// Definition of the COFFEES table
object Reminders extends Table[Reminder]("reminders") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("userid", O.NotNull)
  def createdAt = column[LocalDateTime]("createdat")
  def repeat = column[String]("repeat")
  def firstTime = column[LocalDateTime]("firsttime}")
  def request = column[String]("request")
  def content = column[String]("content")


  def * : ColumnBase[Reminder] = (id.? ~ userId ~ createdAt ~ repeat ~ firstTime ~ request ~ content) <> (Reminder .apply _, Reminder.unapply _)

  // These are both necessary for auto increment to work with psql
  def autoInc =  userId ~ createdAt ~ repeat ~ firstTime ~ request ~ content returning id

  def addToTable(reminder: Reminder): Reminder = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = Reminders.autoInc.insert(reminder.userId, reminder.createdAt, reminder.repeat, reminder.firstTime, reminder.request, reminder.content)
      return fetch(id).get
    }
  }

  def update(reminder: Reminder): Reminder = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = Reminders.insert(reminder)
      return fetch(id).get
    }
  }

  def fetch(id: Long): Option[Reminder] = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      (for { b <- Reminders if b.id is id} yield b).firstOption
    }
  }

}


