package com.ecfront.ez.framework.module.core

case class EZReq(token: String, loginId: String, loginName: String, role_ids: Map[String, String]) extends com.ecfront.common.Req("-1", loginId, null, null) {}

object EZReq {

  val sysReq = EZReq("-1", "-1", "system", null)

  val anonymousReq = EZReq("0", "0", "anonymous", null)

  val TOKEN="token"

  val ORDER_FIELD = "orderField"
  val ORDER_SORT = "orderSort"
  val CONDITION = "condition"
}