package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Action {
      Ok(views.html.index("Your new application is ready."))
  }

  def reminders = Action { request =>
    Authentication.whoAmI(request) match {
      case Some(screenName) => Results.Redirect(routes.RestUser.userReminders(screenName))
      case _ => Results.Redirect(routes.Authentication.twitterSignIn())
    }
  }

  def contact = Action {
    Ok(views.html.index("Under Development"))
  }

}
