package actors

import akka.actor._
import akka.routing.RoundRobinRouter
import play.Logger
import org.joda.time.DateTime
import helpers.{TwitterApi, TwitterHelpers}

import twitter4j.{Paging, Status}

import play.api.Play.current
import scala.concurrent.duration.Duration

import scala.concurrent.duration.FiniteDuration
import play.libs.Akka
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import play.api.libs.json.JsValue
import models.Reminders
import helpers.TwitterApi.TwitterStatusAndJson


object ReminderListener {

  def calculate(interval: FiniteDuration, nListeners: Integer) {

    // Create an Akka system
    val system = ActorSystem("MentionListener")

    val master = system.actorOf(Props(new ReminderListener(nListeners)), name="masterListener")

    // Schedule a new batch to be run every 30 seconds
    // The new batch is obtained from reminders scheduled for
    // 30 seconds into the future until 60 seconds into the future
    Akka.system().scheduler.schedule(
      Duration.create(0, TimeUnit.MILLISECONDS),
      interval,
      master,
      GetMentions(new DateTime()))
  }
}


case class GetMentions(initialTime: DateTime)

class ReminderListener(nListeners: Integer) extends Actor {

  // The set of actors that handle twitter mentions
  val reminderParserRouter = context.actorOf(
    Props[ReminderParser].withRouter(RoundRobinRouter(nListeners)), name = "reminderParsers")

  override def receive: Receive = {

    case GetMentions(initialTime) =>

      val maxMentionId: Option[Long] = play.api.db.slick.DB.withSession { implicit session =>
          Reminders.getLatestReminderTwitterId()
      }

      val paging: Paging = new Paging()
      paging.setCount(TwitterApi.MAX_TIMELINE_TWEETS)
      if (maxMentionId.isDefined) {
        Logger.info("Getting mentions since twitter id: %s".format(maxMentionId.get))
        paging.setSinceId(maxMentionId.get)
      }

      val mentions = TwitterApi.getMentionsAndJsonTimeline(paging)

      for (mention: TwitterStatusAndJson <- mentions) {
        Logger.info("Sending mention %s to actors".format(mention.status.getId))
        //val json = Converters.getJsonFromStatus(mention)
        reminderParserRouter ! ParseAndHandleMention(mention.status, mention.json) //, json)
      }
  }
}


case class ParseAndHandleMention(mention: Status, json: JsValue)

class ReminderParser extends Actor {

  override def receive: Receive = {

    case ParseAndHandleMention(mention, json) =>

      Logger.info("Actor Handling mention: {} {}", mention, json)

      play.api.db.slick.DB.withSession {
        implicit session =>
          TwitterHelpers.handleMention(mention, json)
      }
  }
}