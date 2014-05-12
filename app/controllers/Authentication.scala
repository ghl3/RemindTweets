package controllers

import play.api.mvc.{Controller, Action}
import helpers.TwitterApi


object Authentication extends Controller {


  def twitter = Action {

    //val twitter = new TwitterFactory().getInstance()
    val callback = "/callback"

    val requestToken = TwitterApi.authenticate(callback) //twitter.getOAuthRequestToken(callback);
    Redirect(requestToken.getAuthenticationURL)
  }

}