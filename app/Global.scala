import actors.ReminderScheduler

import play.api._


object Global extends GlobalSettings {

  override def onStart(app: Application) {

    Logger.info("Starting app")

    Logger.info("Starting listener actors")


    Logger.info("Starting Reminder Scheduler actors")
    ReminderScheduler.calculate(1)

  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}
