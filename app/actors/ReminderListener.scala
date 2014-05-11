package actors

import akka.actor._
import akka.routing.RoundRobinRouter
import play.Logger
import org.joda.time.DateTime
import helpers.TwitterHelpers

import twitter4j.Status

import play.api.Play.current
import scala.concurrent.duration.Duration

import scala.concurrent.duration.FiniteDuration
import play.libs.Akka
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global


object ReminderListener {

  def calculate(interval: FiniteDuration, nListeners: Integer) {

    // Create an Akka system
    val system = ActorSystem("MentionListener")

    val master = system.actorOf(Props(new ReminderListener(nListeners)), name="master")

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

      Logger.info("Getting Mentions...")
      /*
      val mentions = TwitterApi.getMentions.asScala.iterator
      Logger.info("Mentions: %s".format(mentions))
      for (mention <- mentions) {
        reminderParserRouter ! ParseAndHandleMention(mention)
      }
      */
  }
}


case class ParseAndHandleMention(mention: Status)

class ReminderParser extends Actor {

  override def receive: Receive = {
    case ParseAndHandleMention(mention) =>
      play.api.db.slick.DB.withSession {
        implicit session =>
          TwitterHelpers.handleMention(mention)
      }
  }
}