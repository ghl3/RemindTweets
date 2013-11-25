
import models.Reminder
import org.specs2.mutable._

import org.joda.time.LocalDateTime

class TestMentions extends Specification {

  val testString: String = "{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets Remind Me to fish at 2013-12-02', source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false, inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false, contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets', id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000, name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...', isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf', isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333', profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED', profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23, favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)', profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false, lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2, isFollowRequestSent=false}}"

  "Timeline Mention" in {
    val status: twitter4j.Status = twitter4j.json.DataObjectFactory.createStatus(testString)
    Reminder.isReminder(status) must equalTo(true)
  }

  "Properly Parsed" in {

    val textA = "@RemindTweets Remind Me to eat cereal at 2013-12-01"
    val parsed = Reminder.parseStatusText(textA)
    parsed mustNotEqual(None)
    parsed.get.repeat must equalTo("NEVER")
    parsed.get.firstTime must equalTo(LocalDateTime.parse("2013-12-01"))
    parsed.get.request must equalTo("eat cereal")

    }

}
