package actors

import akka.actor.Actor
import play.Logger

case class TweetRequest(scheduledReminderId: Long, screenName: String, content: String)

case class ReminderSuccess(scheduledReminderId: Long)
case class ReminderFailure(scheduledReminderId: Long)

class TweetSender extends Actor {

  override def receive = {
    case TweetRequest(scheduledReminderId, screenName, content) =>
      try {

        Logger.debug("Received Tweet to send: %s %s %s".format(scheduledReminderId, screenName, content))

        val status: String = "%s %s".format(screenName, content)
        Logger.info("Sending tweet '{}'", status)
        sender ! ReminderSuccess(scheduledReminderId)
      } catch {
        case e: Exception => sender ! ReminderFailure(scheduledReminderId)
      }
    case x => Logger.error("Unknown message received by TweetSender: %s" format x)
  }
}
