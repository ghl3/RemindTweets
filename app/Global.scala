import play.{Logger, GlobalSettings}


class Global extends GlobalSettings {

  @Override
  def onStart(app: App) {

    //lazy val database = Database.forDataSource(DB.getDataSource())

  }

  @Override
  def onStop(app: App) {
    Logger.info("Application shutdown...")
  }

}
