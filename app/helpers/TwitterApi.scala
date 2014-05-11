package helpers

import twitter4j._

import twitter4j.conf.Configuration
import play.Logger


object TwitterApi {


  def getConfigVariableFromEnv: Map[String,String] = {
    Map("consumerKey" -> System.getenv("twitter4j_oauth_consumerKey"),
      "consumerSecret" -> System.getenv("twitter4j_oauth_consumerSecret"),
      "accessToken" -> System.getenv("twitter4j_oauth_accessToken"),
      "accessTokenSecret" -> System.getenv("twitter4j_oauth_accessTokenSecret"))
  }

  def getConfig: Configuration = {

    val vars = getConfigVariableFromEnv

    new twitter4j.conf.ConfigurationBuilder()
      .setOAuthConsumerKey(vars.getOrElse("consumerKey", null))
      .setOAuthConsumerSecret(vars.getOrElse("consumerSecret", null))
      .setOAuthAccessToken(vars.getOrElse("accessToken", null))
      .setOAuthAccessTokenSecret(vars.getOrElse("accessTokenSecret", null))
      .setJSONStoreEnabled(true)
      .setDebugEnabled(true)
      .setUseSSL(true)
      .build()
  }


  def getTwitter: Twitter = {
    val tf = new TwitterFactory(getConfig)
    tf.getInstance()
  }


  def updateStatus(text: String) {
    getTwitter.updateStatus(text)
  }


  def getTwitterStream = {
    new TwitterStreamFactory(getConfig).getInstance()
  }


  def startListener() = {
    val twitterStream = getTwitterStream

    twitterStream.addListener(new TweetListener((status: Status) => {
      print(status.getText)
    }))

    // sample() method internally creates a thread
    // which manipulates TwitterStream
    // and calls these adequate listener methods continuously.
    twitterStream.sample()
  }

  def getMentions = {
    getTwitter.getMentionsTimeline
  }

  def getHomeTimeline = {
   getTwitter.getHomeTimeline
  }


  def getUserTimeline(screenName: String) = {
    getTwitter.getUserTimeline(screenName)
  }


  class TweetListener(val statusAction: (Status)=>Unit) extends StatusListener {

    def onStatus(status: Status) {
      statusAction(status)
    }

    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}

    def onTrackLimitationNotice(numberOfLimitedStatuses: Integer) {}

    def onException(ex: Exception) {
      ex.printStackTrace()
    }

    def onTrackLimitationNotice(p1: Int) {}

    def onStallWarning(p1: StallWarning) {}

    def onScrubGeo(p1: Long, p2: Long) {}
  }


}
