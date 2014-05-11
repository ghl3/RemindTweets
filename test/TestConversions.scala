

import helpers.{TwitterApi, Converters}
import org.scalatest.junit.JUnitSuite
import org.junit.Test
import play.Logger

import helpers.TwitterApi.Status

class TestConversions extends JUnitSuite {


  val dummyJsonALong: String = """{"contributors":null,"text":"@remindtweets Remind me to build this app on Wednesday at 5:00PM","geo":null,"retweeted":false,"in_reply_to_screen_name":"remindtweets","truncated":false,"lang":"en","entities":{"symbols":[],"urls":[],"hashtags":[],"user_mentions":[{"id":2205796142,"name":"George","indices":[0,13],"screen_name":"remindtweets","id_str":"2205796142"}]},"in_reply_to_status_id_str":null,"id":462684493380194304,"source":"web","in_reply_to_user_id_str":"2205796142","favorited":false,"in_reply_to_status_id":null,"retweet_count":0,"created_at":"Sat May 03 20:05:54 +0000 2014","in_reply_to_user_id":2205796142,"favorite_count":0,"id_str":"462684493380194304","place":null,"user":{"location":"New York","default_profile":true,"profile_background_tile":false,"statuses_count":1777,"lang":"en","profile_link_color":"0084B4","profile_banner_url":"https://pbs.twimg.com/profile_banners/42805000/1353359682","id":42805000,"following":false,"protected":false,"favourites_count":28,"profile_text_color":"333333","description":"Ending the world one proton at a time...","verified":false,"contributors_enabled":false,"profile_sidebar_border_color":"C0DEED","name":"Herbie Lewis","profile_background_color":"C0DEED","created_at":"Wed May 27 03:28:35 +0000 2009","is_translation_enabled":false,"default_profile_image":false,"followers_count":127,"profile_image_url_https":"https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg","geo_enabled":false,"profile_background_image_url":"http://abs.twimg.com/images/themes/theme1/bg.png","profile_background_image_url_https":"https://abs.twimg.com/images/themes/theme1/bg.png","follow_request_sent":false,"entities":{"description":{"urls":[]},"url":{"urls":[{"expanded_url":"http://SpontaneousSymmetry.com","indices":[0,22],"display_url":"SpontaneousSymmetry.com","url":"http://t.co/Wp8W6tZCbf"}]}},"url":"http://t.co/Wp8W6tZCbf","utc_offset":-18000,"time_zone":"Central Time (US & Canada)","notifications":false,"profile_use_background_image":true,"friends_count":461,"profile_sidebar_fill_color":"DDEEF6","screen_name":"HerbieLewis","id_str":"42805000","profile_image_url":"http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg","listed_count":5,"is_translator":false},"coordinates":null}"""

  val dummyJsonA: String =
    """{"contributors":null,"text":"@remindtweets Remind me to build this app on Wednesday at 5:00PM",
      "geo":null,"retweeted":false,"in_reply_to_screen_name":"remindtweets","truncated":false,"lang":"en",
      "entities":{"symbols":[],"urls":[],"hashtags":[],"user_mentions":[{"id":2205796142,"name":"George","indices":[0,13],
      "screen_name":"remindtweets","id_str":"2205796142"}]},"in_reply_to_status_id_str":null,"id":462684493380194304,
      "source":"web","in_reply_to_user_id_str":"2205796142","favorited":false,"in_reply_to_status_id":null,
      "retweet_count":0,"created_at":"Sat May 03 20:05:54 +0000 2014","in_reply_to_user_id":2205796142,
      "favorite_count":0,"id_str":"462684493380194304","place":null,"user":{"location":"New York",
      "default_profile":true,"profile_background_tile":false,"statuses_count":1777,"lang":"en","profile_link_color":"0084B4",
      "profile_banner_url":"https://pbs.twimg.com/profile_banners/42805000/1353359682","id":42805000,"following":false,
     "protected":false,"favourites_count":28,"profile_text_color":"333333","description":"Ending the world one proton at a time...",
      "verified":false,"contributors_enabled":false,"profile_sidebar_border_color":"C0DEED","name":"Herbie Lewis",
      "profile_background_color":"C0DEED","created_at":"Wed May 27 03:28:35 +0000 2009","is_translation_enabled":false,
      "default_profile_image":false,"followers_count":127,
      "profile_image_url_https":"https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg","geo_enabled":false,
      "profile_background_image_url":"http://abs.twimg.com/images/themes/theme1/bg.png",
      "profile_background_image_url_https":"https://abs.twimg.com/images/themes/theme1/bg.png",
      "follow_request_sent":false,"entities":{"description":{"urls":[]},
      "url":{"urls":[{"expanded_url":"http://SpontaneousSymmetry.com","indices":[0,22],
     "display_url":"SpontaneousSymmetry.com","url":"http://t.co/Wp8W6tZCbf"}]}},
      "url":"http://t.co/Wp8W6tZCbf","utc_offset":-18000,"time_zone":"Central Time (US & Canada)",
      "notifications":false,"profile_use_background_image":true,"friends_count":461,
      "profile_sidebar_fill_color":"DDEEF6","screen_name":"HerbieLewis","id_str":"42805000",
      "profile_image_url":"http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg","listed_count":5,
      "is_translator":false},"coordinates":null}""" //.stripMargin


  /*
  val dummyJsonA: String =
    """{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets Remind Me to fish at 2013-12-2',
      |source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false,
      |inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false,
      |contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets',
      |id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000,
      |name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...',
      |isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg',
      |profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf',
      |isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333',
      |profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED',
      |profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23,
      |favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)',
      |profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png',
      |profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false,
      |lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2,
      |isFollowRequestSent=false}}""".stripMargin
*/

  val dummyJsonB: String = """ { "user" : { "screen_name" : "DUMMY" } } """


  @Test
  def stringToJsonA() {
    val json = Converters.getJsonFromString(dummyJsonA)
    val screenName = (json \"user" \"screen_name").as[String]
    assert(screenName === "HerbieLewis")
  }

  @Test
  def stringToJsonB {
    val json = Converters.getJsonFromString(dummyJsonB)
    Logger.info("Dummy json b: {}", json)
    val screenName = (json \ "user" \ "screen_name").as[String]
    assert(screenName=="DUMMY")
  }

  @Test
  def stringToJsonC {
    val status: Status = TwitterApi.createStatusFromJsonString(dummyJsonA)
    assert(status.getUser.getScreenName === "HerbieLewis")
  }

}
