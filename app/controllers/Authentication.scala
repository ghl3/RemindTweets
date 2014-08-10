package controllers

import play.api.mvc._
import helpers.TwitterApi
import play.Logger

// Based on:
// https://github.com/yusuke/sign-in-with-twitter/blob/master/src/main/java/twitter4j/examples/signin/CallbackServlet.java

object Authentication extends Controller {

  def twitter = Action { request =>

    val callback = "http://127.0.0.1:9000/verify"

    Logger.debug("Attempting to authenticate twitter user")
    val requestToken = TwitterApi.authenticate(callback)

    val token = requestToken.getToken
    val tokenSecret = requestToken.getTokenSecret
    val oauth_token = requestToken.getParameter("oauth_token")
    val oauth_verifier = requestToken.getParameter("oauth_verifier")
    val url = requestToken.getAuthenticationURL

    Logger.info("%s %s %s %s %s".format(url, token, tokenSecret, oauth_token, oauth_verifier))

    Redirect(requestToken.getAuthenticationURL).withSession(
      //request.session + ("requestToken" -> requestToken.getToken)
      "requestToken" -> token, "requestTokenSecret" -> tokenSecret
    )
  }

  def twitterVerify = Action { request =>

    Logger.info("Session: {}", request.session)

    val oauthVerifier = request.getQueryString("oauth_verifier")

    if (oauthVerifier.isDefined) {
      val requestToken = request.session.get("requestToken")
      val requestTokenSecret = request.session.get("requestTokenSecret")

      Logger.info("%s %s %s".format(requestToken, requestTokenSecret, oauthVerifier))
      val access = TwitterApi.authenticateToken(requestToken.get, requestTokenSecret.get, oauthVerifier.get)

      Logger.info("Successfully logged in %s %s".format(access.getScreenName, access.getUserId))

      Ok("Authenticated").withSession(
        request.session + ("twitterScreenName" -> access.getScreenName)
      )
    } else {
      Forbidden("Not verified")
    }
  }


  def whoAmI = Action { request =>
    Logger.info("Session: {}", request.session)
    request.session.get("twitterScreenName") match {
      case (Some(screenName)) => Ok(screenName)
      case None => Forbidden("Not verified")
    }
  }


  def isSignedIn(screenName: String, session: Session) = {
    session.get("twitterScreenName") match {
      case (Some(`screenName`)) => true
      case _ => false
    }
  }

}