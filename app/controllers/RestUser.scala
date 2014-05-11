package controllers

import helpers.TwitterApi

import play.api.mvc._
import models._

import play.api.db.slick._
import play.api.libs.json.Json


object RestUser extends Controller {


  def user = Action {
    Ok(views.html.user())
  }

  def userReminders(screenName: String) = DBAction { implicit rs =>

    val user: Option[models.User] = Users.findByScreenName(screenName)

    if (user.isEmpty) {
      NotFound("User with screen_name %s was not found".format(screenName))
    } else {
      val reminders = user.get.getReminders
      Ok(views.html.userReminders(user.get, reminders))
    }
  }


  def userTimeline(screenName: String) = DBAction { implicit rs =>

    val timeline = TwitterApi.getUserTimeline(screenName)

    val format = new java.text.SimpleDateFormat("dd-MM-yyyy")

    val texts = for (status <- timeline) yield Json.obj("status" -> status.getText,
      "createdAt" -> format.format(status.getCreatedAt))

    Ok(Json.arr(texts))
  }


  def userScheduledReminders(screenName: String) = DBAction {
    implicit rs =>

      val user: Option[models.User] = Users.findByScreenName(screenName)

      if (user.isEmpty) {
        NotFound("User with screen_name %s was not found".format(screenName))
      } else {
        val scheduledReminders = user.get.getScheduledReminders
        Ok(views.html.scheduledReminders(scheduledReminders))
      }
  }


  /**
   * Create a new user (if necessary) and see if he has
   * requested any reminders.  If so, we add and schedule them.
   * @param screenName
   * @return
   */
  def checkForReminders(screenName: String) = DBAction {implicit rs =>

    val user = Users.getOrCreateUser(screenName)

    if (user.isEmpty) {
      InternalServerError("Failed to get or create user with screen name %s".format(screenName))
    }
    else {
      // Now that we've got the user, let's get his tweets
      val timeline = TwitterApi.getUserTimelineAndJson(screenName)

      val reminders = Reminders.createRemindersFromUserTwitterStatuses(user.get, timeline)

      Ok(Json.toJson(reminders.map(_.what)))
    }
  }
}
