import play.{Logger, GlobalSettings}

import helpers.TwitterApi

class Global extends GlobalSettings {

  @Override
  def onStart(app: App) {
    Logger.info("Application has started")
    Logger.info("Getting Mentions")
    Logger.info("Mentions: %s", TwitterApi.getMentions)
  }

  @Override
  def onStop(app: App) {
    Logger.info("Application shutdown...")
  }

}
