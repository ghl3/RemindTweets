package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current


case class Reminder(id: Long, user: User, content: Reminder#Content)  {

  def getText: String = {
    content.getText
  }

  class Content {
    def getText : String = {
      "My Text Content"
    }
  }

  /*
  def all: List[Reminder] = {
    val reminder = {
      get[Long]("id") ~
        get[String]("text") map {
        case id~label => Reminder(id, label)
      }
    }

    DB.withConnection { implicit c =>
      SQL("select * from task").as(reminder *)
    }
  }
*/
}
