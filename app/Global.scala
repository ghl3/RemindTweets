import actors.ReminderScheduler

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import play.api._

import play.api.Play.current


import play.api.Play

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    Logger.info("Starting app")

    if (Play.configuration.getBoolean("listenerScheduler.run").getOrElse(false)) {
      Logger.info("Starting listener actors")
    }

    if (Play.configuration.getBoolean("reminderScheduler.run").getOrElse(false)) {
      Logger.info("Starting Reminder Scheduler actors")
      val durationInSeconds = Play.configuration.getInt("reminderScheduler.intervalInSeconds").getOrElse(30)
      ReminderScheduler.calculate(Duration.create(durationInSeconds, TimeUnit.SECONDS), 1)
    }
  }


  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }
}
