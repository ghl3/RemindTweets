package controllers

import play.Logger
import helpers.{ReminderIssuer, TwitterApi, Converters}
import scala.collection.JavaConverters._

import play.api.mvc._
import models._
import org.joda.time.DateTime

import play.api.db.slick._

import play.api.libs.json.Json
import models.Tweet
import scala.Some
import models.Tweets.TweetHelpers

object RestUser extends Controller {


  def user(screenName: String) = DBAction { implicit rs =>

    val user: Option[models.User] = Users.findByScreenName(screenName)

    if (user.isEmpty) {
      NotFound("User with screen_name %s was not found".format(screenName))
    } else {
      val reminders = user.get.getReminders
      Ok(views.html.user(user.get, reminders))
    }
  }


  def userTimeline(screenName: String) = DBAction { implicit rs =>

    val timeline = TwitterApi.getUserTimeline(screenName).asScala

    val format = new java.text.SimpleDateFormat("dd-MM-yyyy")

    val texts = for (status <- timeline) yield Json.obj("status" -> status.getText,
      "createdAt" -> format.format(status.getCreatedAt))

    Ok(Json.arr(texts))
  }


}
