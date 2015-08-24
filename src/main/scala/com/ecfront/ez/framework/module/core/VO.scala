package com.ecfront.ez.framework.module.core

case class EZReq(token: String, loginId: String, loginName: String, rules: List[String],args:Map[String,String]) extends com.ecfront.common.Req("-1", loginId, null, null){}

object EZReq{

  val sysReq = EZReq("-1", "-1", "system", null, null)

  val anonymousReq = EZReq("0", "0", "anonymous", null, null)

}