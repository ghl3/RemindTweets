package controllers

import play.api.mvc._
import helpers.TwitterApi
import play.Logger


// Based on:
// https://github.com/yusuke/sign-in-with-twitter/blob/master/src/main/java/twitter4j/examples/signin/CallbackServlet.java

object Authentication extends Controller {

  def twitterSignIn = Action { request =>

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


  def whoAmI(request: Request[_]) = {
    request.session.get("twitterScreenName")
  }


  def isSignedIn(screenName: String, session: Session) = {
    session.get("twitterScreenName") match {
      case (Some(`screenName`)) => true
      case _ => false
    }
  }

/*
https://github.com/playframework/play-slick/issues/81
http://stackoverflow.com/questions/19780545/play-slick-with-securesocial-running-db-io-in-a-separate-thread-pool
 */


/*
  def TwitterSignIn = {
    Ok("Sign in")
  }

  def AuthenticateMe(screenName: String) = Action { implicit request =>
    if(!Authentication.isSignedIn(screenName, request.session)) {
      Forbidden("Forbidden, yo")
    } else {
      Results.Redirect(routes.Authentication.twitterSignIn)
    }
  }
*/

/*
  class TwitterAuthenticatedRequest[A](val screenName: String, request: Request[A]) extends WrappedRequest[A](request)

  object TwitterAuthenticated extends ActionBuilder[TwitterAuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (TwitterAuthenticatedRequest[A]) => Future[SimpleResult]) = {
      request.session.get("twitterScreenName").map { screenName =>
        block(new TwitterAuthenticatedRequest(screenName, request))
      } getOrElse {
        Future.successful(Forbidden)
      }
    }
  }

  trait Secured {
    private def username(request: RequestHeader) = request.session.get(Security.username)

    private def onUnauthorized(request: RequestHeader) = {
      Results.Redirect("/").flashing("error" -> "You need to login first.")
    }

    def IsAuthenticated(f: => String => DBSessionRequest[_] => SimpleResult) =
      Security.Authenticated(username, onUnauthorized) {
        user => DBAction(rs => f(user)(rs))
      }
  }
*/


}