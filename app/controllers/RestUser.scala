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
    if(!Authentication.isSignedIn(screenName, rs.session)) {
      Forbidden("Forbidden, yo")
    } else {

      // Get an option with the first value or an empty option
      val userOpt = Users.getOrCreateUser(screenName)
      /*
      val userOpt: Option[User] = List(Users.findByScreenName(screenName), Users.createWithScreenName(screenName))
        .find(_.nonEmpty).flatten
*/
      userOpt match {
        case Some(user) =>
          val reminders = user.getReminders
          Ok(views.html.userReminders(user, reminders))
        case None => InternalServerError
      }
    }
  }


  def userScheduledReminders(screenName: String) = DBAction { implicit rs =>

    if (!Authentication.isSignedIn(screenName, rs.session)) {
      Forbidden("Forbidden, yo")
    } else {

      val userOpt = Users.getOrCreateUser(screenName)

      userOpt match {
        case Some(user) =>
          val scheduledReminders = user.getScheduledReminders

          val remindersAndReminder = for (s <- scheduledReminders) yield (s, s.getReminder.get)

          Ok(views.html.scheduledReminders(user, remindersAndReminder))
        case None => InternalServerError
      }

      //val user: Option[models.User] = Users.findByScreenName(screenName)
      /*
      if (user.isEmpty) {
        NotFound("User with screen_name %s was not found".format(screenName))
      } else {
        val scheduledReminders = user.get.getScheduledReminders
        Ok(views.html.scheduledReminders(scheduledReminders))
      }
    }
    */
    }
  }


  def userTimeline(screenName: String) = DBAction { implicit rs =>

    if(!Authentication.isSignedIn(screenName, rs.session)) {
      Forbidden("Forbidden, yo")
    } else {

      val timeline = TwitterApi.getUserTimeline(screenName)

      val format = new java.text.SimpleDateFormat("dd-MM-yyyy")

      val texts = for (status <- timeline) yield Json.obj("status" -> status.getText,
        "createdAt" -> format.format(status.getCreatedAt))

      Ok(Json.arr(texts))
    }
  }



  /**
   * Create a new user (if necessary) and see if he has
   * requested any reminders.  If so, we add and schedule them.
   * @param screenName
   * @return
   */
  def checkForReminders(screenName: String) = DBAction {implicit rs =>

    if(!Authentication.isSignedIn(screenName, rs.session)) {
      Forbidden("Forbidden, yo")
    } else {

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
}
