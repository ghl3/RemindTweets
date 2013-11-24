package models

import org.joda.time.LocalDateTime

import app.MyPostgresDriver.simple._
import org.json4s.JValue

import scala.slick.lifted._

import play.api.Play.current


// SEE: https://github.com/ThomasAlexandre/slickcrudsample/
// http://java.dzone.com/articles/getting-started-play-21-scala

// TODO: Convert to DateTime
case class Tweet(id: Option[Long], content: JValue, fetchedAt: LocalDateTime)

// Definition of the COFFEES table
object Tweets extends Table[Tweet]("tweet") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def content = column[JValue]("content")
  def fetchedAt = column[LocalDateTime]("fetchedAt")

  def * : ColumnBase[Tweet] = (id.? ~ content ~ fetchedAt) <> (Tweet .apply _, Tweet.unapply _)

  def autoInc = content ~ fetchedAt returning id

  def add(json: JValue, fetchedAt: LocalDateTime): Long = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      Tweets.autoInc.insert(json, fetchedAt)
    }
  }

  // Create a new instance
  def create(content: JValue, fetchedAt: LocalDateTime=LocalDateTime.now()) = Tweet(Option(15L), content, fetchedAt)
}
