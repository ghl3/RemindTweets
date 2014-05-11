package models

import org.joda.time.LocalDateTime
import app.MyPostgresDriver.simple._

import helpers.TwitterApi


case class User(id: Option[Long], screenName: String, createdAt: LocalDateTime) {


  def getReminders()(implicit s: Session): List[Reminder] = {
    Reminders.reminders.where(_.userId === id).list
  }

  def getScheduledReminders(id: Long)(implicit s: Session): List[ScheduledReminder] = {
    ScheduledReminders.scheduledReminders.where(_.userId === id).list
  }
}


class Users(tag: Tag) extends Table[User](tag, "users") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def screenName = column[String]("screen_name", O.NotNull)
  def createdAt = column[LocalDateTime]("createdat")

  def * = (id.?, screenName, createdAt) <> (User.tupled, User.unapply _)

}


object Users {

  val users = TableQuery[Users]

  def findById(id: Long)(implicit s: Session): Option[User] = {
    users.where(_.id === id).firstOption
  }

  def insert(user: User)(implicit s: Session) {
    users.insert(user)
  }

  def insertAndGet(user: User)(implicit s: Session): User = {
    val userId = (users returning users.map(_.id)) += user
    user.copy(id = Some(userId))
  }

  def update(id: Long, user: User)(implicit s: Session) {
    val userToUpdate: User = user.copy(Some(id))
    users.where(_.id === id).update(userToUpdate)
  }

  def delete(id: Long)(implicit s: Session) {
    users.where(_.id === id).delete
  }

  def exists(id: Long)(implicit s: Session): Boolean = {
    findById(id).nonEmpty
  }

  def findByScreenName(screenName: String)(implicit s: Session): Option[User] = {
    users.where(_.screenName === screenName).firstOption
  }

  def createWithScreenName(screenName: String) (implicit s: Session): Option[User] = {
    try {
      val user = User(None, screenName, LocalDateTime.now())
      Some(insertAndGet(user))
    } catch {
      case e: Exception => None
    }
  }

  def createUser(user: TwitterApi.User)(implicit s: Session): User = {
    insertAndGet(new User(None, user.getScreenName, LocalDateTime.now()))
  }

  def getOrCreateUser(screenName: String)(implicit s: Session): Option[User] = {
    findByScreenName(screenName) match {
      case Some(user) => Some(user)
      case None => createWithScreenName(screenName)
    }
  }
}

