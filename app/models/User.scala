package models

import org.joda.time.LocalDateTime
//import scala.slick.lifted._
//import helpers.Database.getDatabase
import app.MyPostgresDriver.simple._
//import scala.slick.lifted.Tag

//import app.MyPostgresDriver.simple.Tag


// TODO: Add twitterid
case class User(id: Option[Long], screenName: String, createdAt: LocalDateTime)

  /*
  def getReminders: List[Reminder] = {
    getDatabase().withSession { implicit session =>
      return (for { b <- Reminders if b.userId is this.id} yield b).list
    }
  }

  def getScheduledReminders: List[ScheduledReminder] = {
    getDatabase().withSession { implicit session: Session =>
      return (for { b <- ScheduledReminders if b.userId is this.id} yield b).list
    }
  }
*/


/*
object User {

  def createUser(user: twitter4j.User): User = {
    return new User(None, user.getScreenName, LocalDateTime.now())
  }

 def isNewUser(user: twitter4j.User): Boolean = {
   return false
 }

}
*/


// Definition of the COFFEES table
class Users(tag: Tag) extends Table[User](tag, "users") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def screenName = column[String]("screenname", O.NotNull)
  def createdAt = column[LocalDateTime]("createdat")

  def * = (id.?, screenName, createdAt) <> (User.tupled, User.unapply _)

/*
  // These are both necessary for auto increment to work with psql
  def autoInc = screenName ~ createdAt returning id

  def addToTable(user: User): User = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = Users.autoInc.insert(user.screenName, user.createdAt)
      return fetch(id).get
    }
  }

  def update(user: User): User = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = Users.insert(user)
      return fetch(id).get
    }
  }

  def fetch(id: Long): Option[User] = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      (for { b <- Users if b.id is id} yield b).firstOption
    }
  }
*/
}

