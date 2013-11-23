package helpers

import twitter4j._

import twitter4j.conf.{ConfigurationBuilder, Configuration}

object TwitterApi {


  def getConfig: Configuration = {
    new twitter4j.conf.ConfigurationBuilder()
      .setOAuthConsumerKey("[your consumer key here]")
      .setOAuthConsumerSecret("[your consumer secret here]")
      .setOAuthAccessToken("[your access token here]")
      .setOAuthAccessTokenSecret("[your access token secret here]")
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
