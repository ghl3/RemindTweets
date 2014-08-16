package helpers

import play.api.Play
import twitter4j._

import twitter4j.conf.Configuration
import play.api.libs.json.JsValue

import scala.collection.JavaConverters._
import twitter4j.auth.RequestToken

import play.Logger

object TwitterApi {


  // We import types from the TwitterApi to be able to
  // manage where and how they are used
  type Status = twitter4j.Status
  type Paging = twitter4j.Paging
  type User = twitter4j.User
  type ResponseList[T] = twitter4j.ResponseList[T]


  val MAX_TIMELINE_TWEETS = 800

  case class TwitterStatusAndJson(status: twitter4j.Status, json: JsValue) {
    def this(status: twitter4j.Status) {
      this(status, TwitterApiInternal.getJsonFromStatus(status))
    }
  }

  def sendTweetToUser(screenName: String, content: String) = {
    val status = "%s %s".format(screenName, content)

    if (Play.configuration.getBoolean("sendTweets").getOrElse(false)) {
      TwitterApiInternal.updateStatus(status)
    } else {
      Logger.info("Mock Sending Tweet: {}", status);
    }
  }


  def getScreenName = TwitterApiInternal.getTwitter.getScreenName

  def getId = TwitterApiInternal.getTwitter.getId

  def getMentionsTimeline = {
    for (mention <- TwitterApiInternal.getTwitter.getMentionsTimeline.asScala.view) yield mention
  }

  def getMentionsAndJsonTimeline = {
    for (mention <- TwitterApiInternal.getTwitter.getMentionsTimeline.asScala.view) yield new TwitterStatusAndJson(mention)
  }

  def getMentionsTimeline(paging: Paging) = {
    for (mention <- TwitterApiInternal.getTwitter.getMentionsTimeline(paging).asScala.view) yield mention
  }

  def getMentionsAndJsonTimeline(paging: Paging) = {
    for (mention <- TwitterApiInternal.getTwitter.getMentionsTimeline(paging).asScala.view) yield new TwitterStatusAndJson(mention)
  }

  def getHomeTimeline = {
    for (mention <- TwitterApiInternal.getTwitter.getHomeTimeline.asScala.view) yield mention
  }

  def getHomeTimelineAndJson = {
    for (mention <- TwitterApiInternal.getTwitter.getHomeTimeline.asScala.view) yield new TwitterStatusAndJson(mention)
  }

  def getUserTimeline(screenName: String) = {
    for (mention <- TwitterApiInternal.getTwitter.getUserTimeline(screenName).asScala.view) yield mention
  }

  def getUserTimelineAndJson(screenName: String) = {
    for (mention <- TwitterApiInternal.getTwitter.getUserTimeline(screenName).asScala.view) yield new TwitterStatusAndJson(mention)
  }

  def createStatusFromJsonString(jsonString: String): Status = {
    twitter4j.json.DataObjectFactory.createStatus(jsonString)
  }

  def authenticate(callback: String): RequestToken = {
    TwitterApiInternal.getAuthFactory.getOAuthRequestToken(callback)
  }

  def authenticateToken(token: String, tokenSecret: String, verifier: String) = {
    val requestToken: RequestToken = new RequestToken(token, tokenSecret)
    TwitterApiInternal.getAuthFactory.getOAuthAccessToken(requestToken, verifier)
  }


  def getTwitterStream = {
    TwitterApiInternal.getTwitterStream
  }


  /**
   * We encapsulate the actual twitter api
   * for a number of reasons (testing,
   * controlling thread safety, etc)
   */
  private object TwitterApiInternal {

    def getConfigVariableFromEnv: Map[String, String] = {
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


    def getAuthConfig: Configuration = {

      val vars = getConfigVariableFromEnv

      new twitter4j.conf.ConfigurationBuilder()
        .setOAuthConsumerKey(vars.getOrElse("consumerKey", null))
        .setOAuthConsumerSecret(vars.getOrElse("consumerSecret", null))
        .setPrettyDebugEnabled(true)
        .setUseSSL(true)
        .build()
    }


    def getAuthFactory: Twitter = {
      val tf = new TwitterFactory(getAuthConfig)
      tf.getInstance()
    }


    // Easy wrappers for JSON conversion

    def getJsonStringFromStatus(status: twitter4j.Status): String = {
      twitter4j.json.DataObjectFactory.getRawJSON(status)
    }

    def getJsonFromStatus(status: twitter4j.Status): JsValue = {
      Converters.getJsonFromString(twitter4j.json.DataObjectFactory.getRawJSON(status))
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
  }

  class TweetListener(val statusAction: (Status) => Unit) extends StatusListener {

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
