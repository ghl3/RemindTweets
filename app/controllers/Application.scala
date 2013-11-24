package controllers

import play.api.Play.current

import play.Logger
import play.api.mvc.{Controller, Action}
import helpers.TwitterApi
import scala.collection.JavaConverters._


import models.{Tweet, Tweets}
import org.json4s._
import org.json4s.native.JsonMethods

import play.api.db.slick.Config.driver.simple._

import org.joda.time.LocalDateTime


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def mentions = Action {

    Logger.info("Getting status for {} {}", TwitterApi.getTwitter.getScreenName, TwitterApi.getTwitter.getId: java.lang.Long)

    for (status: twitter4j.Status <- TwitterApi.getHomeTimeline.asScala) {
      Logger.info("Status: {}", status.getUser)
    }
    Logger.info("getMentions: {}", TwitterApi.getMentions.toString)
    val mentionList = TwitterApi.getMentions.listIterator.asScala
    Ok(views.html.mentions(mentionList))
  }


  def addCoffee = Action {

    val myVal = JsonMethods.parse(""" { "numbers" : [1, 2, 3, 4] } """)

    play.api.db.slick.DB.withSession{implicit session: Session =>
      Tweets.add(myVal, LocalDateTime.now())
    }

    Ok(views.html.index("Your new application is ready."))
  }


  /*
  def test = Action {

    def instance = Action {
      Database.forDataSource(DB.getDataSource()) withSession {
        val q = Retailer.map(_.name)
        Ok(views.html.instance(q.list, newRForm))
      }
    }


  }
  */

}