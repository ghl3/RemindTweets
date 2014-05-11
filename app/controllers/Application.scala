package controllers

import play.Logger
import helpers.{TwitterApi, Converters}

import app.MyPostgresDriver.simple._

import play.api.mvc._
import models._
import org.joda.time.DateTime

import play.api.db.slick._

import play.api.libs.json.Json
import models.Tweet
import scala.Some


object Application extends Controller {


  def index = Action {
      Ok(views.html.index("Your new application is ready."))
  }

  def tweet(id: Long) = DBAction { implicit rs =>
    Ok(Tweets.findById(id).get.content)
  }


  def addTweet(userId: Long) = DBAction(parse.json) { implicit rs =>
    Users.findById(userId) match {
      case Some(user) =>
        val myTweet = Tweet(Option.empty, userId, 12345L, "FOO", Json.parse("{}"), new DateTime())
        val updatedTweet = Tweets.insertAndGet(myTweet)
        Ok(views.html.index("Your new application is ready: " + updatedTweet))
      case None =>
        NotFound("User with id %s was not found".format(userId))
    }
  }

  /**
   * First, we check for an existing user of that name.
   * If one doesn't exist, we create one.
   * Next, we get all the tweets for that user.
   * We then scan through those tweets for any reminders.
   * If we find any reminders, we save the tweets and
   * create corresponding reminders.
   * // TODO: Separate this into to end points for creating the user and adding the reminders
   * @param screenName
   * @return
   */
  def checkForReminders(screenName: String) = DBAction { implicit rs =>

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


  def mentions = DBAction { implicit rs =>

    Logger.info("Getting status for {} {}", TwitterApi.getScreenName, TwitterApi.getId: java.lang.Long)

    val mentions = TwitterApi.getMentionsTimeline
    Logger.info("Mentions: %s".format(mentions))

    Logger.info("Putting into mentions")
    Ok(views.html.mentions(mentions.iterator))
  }


  def testMentions = Action {
    val status = TwitterApi.createStatusFromJsonString(Converters.dummyJsonA)

    Logger.info("Status: {}", status)
    Ok(views.html.index("Fish"))
  }


  def testReminder = DBAction { implicit rs =>

    val remindersAndUsers = for {
      r <- Reminders.reminders
      u <- r.user
    } yield (r.id, u.id)

    remindersAndUsers.list.foreach{case(r, u) => Logger.info("Sum: %s".format(r+u))}

    Ok("SUP")
  }

}
