package controllers

import play.api.mvc.{Controller, Action}
import helpers.TwitterApi
import play.Logger


object Authentication extends Controller {

  def twitter = Action {request =>

    val callback = "http://127.0.0.1:9000/verify"

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


  def twitterVerify = Action {request =>

    Logger.info("Session: {}", request.session)

    val oauthVerifier = request.getQueryString("oauth_verifier")

    if (oauthVerifier.isDefined) {
      val requestToken = request.session.get("requestToken")
      val requestTokenSecret = request.session.get("requestTokenSecret")

      Logger.info("%s %s %s".format(requestToken, requestTokenSecret, oauthVerifier))
      val access = TwitterApi.authenticateToken(requestToken.get, requestTokenSecret.get, oauthVerifier.get)

      Logger.info("Successfully logged in %s %s".format(access.getScreenName, access.getUserId))
    }

    Ok("SUP")
  }

}