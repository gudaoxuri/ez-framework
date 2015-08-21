package com.ecfront.ez.framework.module.auth


object AuthService {

  val sysReq = AuthReq("-1", "-1", "system", null, null)

  val anonymousReq = AuthReq("0", "0", "anonymous", null, null)

}

case class AuthReq(token: String, loginId: String, loginName: String, rules: List[String],args:Map[String,String]) extends com.ecfront.common.Req("-1", loginId, null, null){}

