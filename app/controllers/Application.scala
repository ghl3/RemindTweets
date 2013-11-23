package controllers

import play.Logger
import play.api.mvc.{Controller, Action}
import helpers.TwitterApi
import scala.collection.JavaConverters._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def mentions = Action {

    Logger.info("Getting status for {} {}", TwitterApi.getTwitter.getScreenName, TwitterApi.getTwitter.getId: java.lang.Long)

    for (status: twitter4j.Status <- TwitterApi.getHomeTimeline.asScala) {
      Logger.info("Status: {}", status.getUser)
    }
    Logger.info("getMentions: {}", TwitterApi.getMentions.toString)
    val mentionList = TwitterApi.getMentions.listIterator.asScala
    Ok(views.html.mentions(mentionList))
  }

}