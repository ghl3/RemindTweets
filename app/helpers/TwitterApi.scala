package helpers

import twitter4j._

import twitter4j.conf.{ConfigurationBuilder, Configuration}
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
      .build()
  }


  def getStatusFromJson(jsonString: String): twitter4j.Status = {
    twitter4j.json.DataObjectFactory.createStatus(jsonString)
  }

  def getJsonFromStatus(status: twitter4j.Status): String = {
    var statusJson: String = twitter4j.json.DataObjectFactory.getRawJSON(status)
    if (statusJson==null) {
      Logger.error("Could not get json from status: {}", status)
      statusJson = dummyJson
    }
    return statusJson
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

  val dummyJson: String = "{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets This looks interesting', source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false, inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false, contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets', id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000, name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...', isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf', isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333', profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED', profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23, favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)', profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false, lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2, isFollowRequestSent=false}}"


}
