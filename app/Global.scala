import play.{Logger, GlobalSettings}

import helpers.Twitter

class Global extends GlobalSettings {

  @Override
  def onStart(app: Application) {
    Logger.info("Application has started")
    Twitter.startListener()
  }

  @Override
  def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}
