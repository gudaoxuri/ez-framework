package com.ecfront.ez.framework.service.oauth2

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.EZ_Account
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.auth.oauth2.{AccessToken, OAuth2FlowType}

object GitHubProcessor extends AppProcessor {

  override def getAccount(accessToken: AccessToken): Resp[EZ_Account] = {
    oauth2.api(HttpMethod.GET, "/users",
      new JsonObject().put("access_token", accessToken.principal().getString("access_token")), new Handler[AsyncResult[JsonObject]] {
        override def handle(e: AsyncResult[JsonObject]): Unit = {
          // TODO
        }
      })
    Resp.success(null)
  }

  override def site: String = "https://github.com/login"

  override def authorizationPath: String = "/oauth/authorize"

  override def accessTokenPath: String = "/oauth/access_token"

  override def refreshTokenPath: String = "/oauth/access_token"

  override def scope: String = ""

  override protected def appName: String = "github"

  override def flowType: OAuth2FlowType = OAuth2FlowType.CLIENT
}
