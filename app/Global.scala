import actors.{ReminderListener, ReminderScheduler}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import play.api._

import play.api.Play.current

import play.api.Play


object Global extends GlobalSettings {

  /*
  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    val modeSpecificConfig = config ++ Configuration(ConfigFactory.load(s"application.${mode.toString.toLowerCase}.conf"))
    super.onLoadConfig(modeSpecificConfig, path, classloader, mode)
  }
*/

  override def onStart(app: Application) {

    Logger.info("Starting app")

    if (Play.configuration.getBoolean("listenerScheduler.run").getOrElse(false)) {
      Logger.info("Starting listener actors")
      val durationInSeconds = Play.configuration.getInt("reminderListener.intervalInSeconds").getOrElse(30)
      ReminderListener.calculate(Duration.create(durationInSeconds, TimeUnit.SECONDS), 1)
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
