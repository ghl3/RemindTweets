package helpers

import play.Logger
import play.api.libs.json.JsValue


object Converters {


  def createStatusFromJsonString(jsonString: String): twitter4j.Status = {
    return twitter4j.json.DataObjectFactory.createStatus(jsonString)
  }

  def createStatusFromJson(json: JsValue): twitter4j.Status = {
    try {
      //implicit val formats =org.json4s.DefaultFormats
      val jsonString: String = json.toString() // extract[String] //getJsonStringFromJson(json) //json.extract[String]
      return createStatusFromJsonString(jsonString)
    }
    catch {
      case e: Exception =>
        Logger.error("Failed to create twitter status", e)
        return null
    }
  }

  def getJsonStringFromStatus(status: twitter4j.Status): String = {
    var statusJson: String = twitter4j.json.DataObjectFactory.getRawJSON(status)
    if (statusJson==null) {
      Logger.error("Could not get json from status: {}", status)
      statusJson = dummyJsonB
    }
    return statusJson
  }


  def getJsonFromString(json: String): org.json4s.JValue = {
    org.json4s.native.JsonParser.parse(json)
    //JsonMethods.parse(json)
  }

  def getJsonStringFromJson(json: org.json4s.JValue): String = {
    implicit val formats =org.json4s.DefaultFormats
    json.extract[String]
  }

  val dummyJsonA: String = """{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets Remind Me to fish at 2013-12-2', source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false, inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false, contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets', id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000, name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...', isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf', isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333', profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED', profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23, favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)', profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false, lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2, isFollowRequestSent=false}}"""
  val dummyJsonB: String = """ { "screenName" : "DUMMY" } """

  //val dummyJson: String = "{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets This looks interesting', source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false, inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false, contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets', id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000, name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...', isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf', isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333', profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED', profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23, favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)', profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false, lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2, isFollowRequestSent=false}}"


}
