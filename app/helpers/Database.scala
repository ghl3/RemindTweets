package helpers

object Database {

  def getDatabase() = {
    app.MyPostgresDriver.simple.Database.forURL("jdbc:postgresql://localhost:5432/remindtweets?user=remindtweets", driver="org.postgresql.Driver")
  }

}