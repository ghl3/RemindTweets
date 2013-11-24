package controllers

import play.Logger
import play.api.mvc.{Controller, Action}
import helpers.TwitterApi
import scala.collection.JavaConverters._

import models.{Tweet, Tweets}
import org.json4s._
import org.json4s.native.JsonMethods

import org.joda.time.LocalDateTime


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def mentions = Action {

    Logger.info("Getting status for {} {}", TwitterApi.getTwitter.getScreenName, TwitterApi.getTwitter.getId: java.lang.Long)

    val mentions = TwitterApi.getMentions.asScala.iterator
    Logger.info("Mentions: %s".format(mentions))

    mentions.foreach({(status: twitter4j.Status) =>
      var tweet = Tweet.fromStatus(status)
      tweet = Tweets.addToTable(tweet)
      Logger.info("Tweet: %s %s".format(tweet.id, tweet.jsonString))
    })

    Logger.info("Putting into mentions")
    Ok(views.html.mentions(mentions))
  }


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


}