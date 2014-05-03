package controllers

import play.Logger
import helpers.{ReminderIssuer, TwitterApi, Converters}
import scala.collection.JavaConverters._

import play.api.mvc._
import models._
import org.joda.time.DateTime

import play.api.db.slick._

import play.api.libs.json.Json
import models.Tweet
import scala.Some
import models.Tweets.TweetHelpers


object Application extends Controller {

  def index = Action {
      Ok(views.html.index("Your new application is ready."))
  }


  def tweet(id: Long) = DBAction { implicit rs =>
    Ok(Tweets.findById(id).get.content)
  }


  def addTweet(userId: Long) = DBAction(parse.json) { implicit rs =>
    Users.findById(userId) match {
      case Some(user) =>
        val myTweet = Tweet(Option.empty, userId, 12345L, "FOO", Json.parse("{}"), new DateTime())
        val updatedTweet = Tweets.insertAndGet(myTweet)
        Ok(views.html.index("Your new application is ready: " + updatedTweet))
      case None =>
        NotFound("User with id %s was not found" format userId)
    }
  }


  def userTimeline(screenName: String) = DBAction { implicit rs =>

    val timeline = TwitterApi.getUserTimeline(screenName).asScala

    val format = new java.text.SimpleDateFormat("dd-MM-yyyy")

    val texts = for (status <- timeline) yield Json.obj("status" -> status.getText,
      "createdAt" -> format.format(status.getCreatedAt))

    Ok(Json.arr(texts))
  }


  /**
   * First, we check for an existing user of that name.
   * If one doesn't exist, we create one.
   * Next, we get all the tweets for that user.
   * We then scan through those tweets for any reminders.
   * If we find any reminders, we save the tweets and
   * create corresponding reminders.
   * // TODO: Separate this into to end points for creating the user and adding the reminders
   * @param screenName
   * @return
   */
  def checkForReminders(screenName: String) = DBAction { implicit rs =>

    val user = Users.getOrCreateUser(screenName)

    if (user.isEmpty) {
      InternalServerError("Failed to get or create user with screen name %s".format(screenName))
    }
    else {
      // Now that we've got the user, let's get his tweets
      val timeline: Iterable[twitter4j.Status] = TwitterApi.getUserTimeline(screenName).asScala

      Reminders.createRemindersFromUserTwitterStatuses(user.get, timeline)
      Ok("SUP")

    }
  }


  def mentions = DBAction { implicit rs =>

    Logger.info("Getting status for {} {}", TwitterApi.getTwitter.getScreenName, TwitterApi.getTwitter.getId: java.lang.Long)

    val mentions = TwitterApi.getMentions.asScala.iterator
    Logger.info("Mentions: %s".format(mentions))

    for (mention <- mentions) {

      val jsonString = Converters.getJsonStringFromStatus(mention)
      Logger.info("Got JSON String: {}", jsonString)


      val json = Converters.getJsonFromStatus(mention)
      Logger.info("Got JSON: {}", json)

      // Check if it's an existing user
      val user = Users.getOrCreateUser(mention.getUser.getScreenName)

      Logger.info("Handling mention: {} for user {}", mention, user)

      val tweet = TweetHelpers.fromStatus(user.get, mention)
      val parsed = ReminderHelper.parseStatusText(mention.getText)

      Reminders.createAndSaveIfReminder(user.get, tweet, parsed)
    }

    Logger.info("Putting into mentions")
    Ok(views.html.mentions(mentions))
  }


  def testMentions = Action {
    //val testString: String = "{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets Remind Me to fish at 2013-12-2', source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false, inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false, contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets', id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000, name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...', isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf', isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333', profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED', profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23, favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)', profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false, lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2, isFollowRequestSent=false}}"
    val status = Converters.createStatusFromJsonString(Converters.dummyJsonA)

    Logger.info("Status: {}", status)
    Ok(views.html.index("Fish"))
  }


  def issueReminders = DBAction { implicit rs =>
    ReminderIssuer.issueReminders
    Ok("SUP")
  }

/*
  def addTweet = Action {
    val myVal = JsonMethods.parse(""" { "numbers" : [1, 2, 3, 4] } """)
    val tweet: Tweet = Tweet(None, 999L, "MrDood", myVal, LocalDateTime.now())
    val id: Long = Tweets.addToTable(tweet).id.get
    Ok(views.html.index("Created Tweet: $id"))
  }

  def getTweet(id: Long) = Action {
    val tweet = Tweets.fetch(id).get.getStatus
    Ok(views.html.index("Created Tweet: %s".format(tweet.toString)))
  }
*/

}
