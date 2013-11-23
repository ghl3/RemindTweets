package helpers

import twitter4j._


class Twitter {


  def getConfig() {
    new twitter4j.conf.ConfigurationBuilder()
      .setOAuthConsumerKey("[your consumer key here]")
      .setOAuthConsumerSecret("[your consumer secret here]")
      .setOAuthAccessToken("[your access token here]")
      .setOAuthAccessTokenSecret("[your access token secret here]")
      .build
  }


  def updateStatus(text: String) {
    val twitter = TwitterFactory.getSingleton();
    val status = twitter.updateStatus(text);
    return status
  }

  /*
  def getMentions() {
    val mentions = status.getUserMentionEntities();
    if (mentions != null && mentions.length > 0) {
      // The tweet has some mentions.
    } else {
      // does not have
    }
  }
*/

  class TweetListener(val statusAction: (Status)=>Void ) extends StatusListener {

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


  def streaming() {

    val twitterStream = new TwitterStreamFactory().getInstance()
    twitterStream.addListener(new TweetListener((status: Status) => {
      print(status.getText)
      return
    }))

    // sample() method internally creates a thread
    // which manipulates TwitterStream
    // and calls these adequate listener methods continuously.
    twitterStream.sample()


  }


}
