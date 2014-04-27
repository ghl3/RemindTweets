package controllers

import play.Logger
import helpers.TwitterApi
import scala.collection.JavaConverters._

import helpers.ReminderCreation.handleStatus
import helpers.Converters

import play.api._
import play.api.mvc._



object Application extends Controller {

  def index = Action {

      Ok(views.html.index("Your new application is ready."))
  }


  def mentions = Action {

    Logger.info("Getting status for {} {}", TwitterApi.getTwitter.getScreenName, TwitterApi.getTwitter.getId: java.lang.Long)

    val mentions = TwitterApi.getMentions.asScala.iterator
    Logger.info("Mentions: %s".format(mentions))

    mentions.foreach({(status: twitter4j.Status) =>
      Logger.info("Handling status: {}", status)
      handleStatus(status)
      //var tweet = Tweet.fromStatus(status)
      //tweet = Tweets.addToTable(tweet)
      //Logger.info("Tweet: %s %s".format(tweet.id, tweet.jsonString))
    })

    Logger.info("Putting into mentions")
    Ok(views.html.mentions(mentions))
  }

  def testMentions = Action {
    //val testString: String = "{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets Remind Me to fish at 2013-12-2', source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false, inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false, contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets', id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000, name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...', isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf', isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333', profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED', profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23, favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)', profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false, lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2, isFollowRequestSent=false}}"
    val status = Converters.createStatusFromJsonString(Converters.dummyJsonA)

// val status = twitter4j.json.DataObjectFactory.createStatus(testString)
    Logger.info("Status: {}", status)
    handleStatus(status)
    Ok(views.html.index("Fish"))
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
