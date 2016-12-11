package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.core.rpc.AutoBuildingProcessor
import com.ecfront.ez.framework.core.rpc.apidoc.APIDocProcessor
import com.fasterxml.jackson.databind.JsonNode

import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  val EB_ORG_ADD_FLAG = "/ez/auth/rbac/organization/add/"
  val EB_ORG_REMOVE_FLAG = "/ez/auth/rbac/organization/remove/"
  val EB_RESOURCE_ADD_FLAG = "/ez/auth/rbac/resource/add/"
  val EB_RESOURCE_REMOVE_FLAG = "/ez/auth/rbac/resource/remove/"
  val EB_ROLE_ADD_FLAG = "/ez/auth/rbac/role/add/"
  val EB_ROLE_REMOVE_FLAG = "/ez/auth/rbac/role/remove/"

  val EB_ORG_INIT_FLAG = "/ez/auth/organizationInit/"
  val EB_LOGIN_SUCCESS_FLAG = "/ez/auth/loginSuccess/"
  val EB_LOGOUT_FLAG = "/ez/auth/logout/"

  val EB_FLUSH_FLAG = "/ez/gateway/auth/flush/"

  var customLogin: Boolean = _
  var defaultOrganizationCode: String = _
  var loginKeepSeconds: Int = _
  var loginLimit_showCaptcha: Int = _
  var encrypt_algorithm: String = _
  var encrypt_salt: String = _

  override def init(parameter: JsonNode): Resp[String] = {
    customLogin = parameter.path("customLogin").asBoolean(false)
    if (parameter.has("loginLimit")) {
      val loginLimit = parameter.get("loginLimit")
      if (loginLimit.has("showCaptcha")) {
        loginLimit_showCaptcha = loginLimit.get("showCaptcha").asInt()
      }
    } else {
      loginLimit_showCaptcha = Int.MaxValue
    }
    defaultOrganizationCode = parameter.path("defaultOrganizationCode").asText("")
    loginKeepSeconds = parameter.path("loginKeepSeconds").asInt(0)
    encrypt_algorithm =
      if (parameter.has("encrypt") && parameter.get("encrypt").has("algorithm")) {
        parameter.get("encrypt").get("algorithm").asText()
      } else {
        "SHA-256"
      }
    encrypt_salt =
      if (parameter.has("encrypt") && parameter.get("encrypt").has("salt")) {
        parameter.get("encrypt").get("salt").asText()
      } else {
        ""
      }
    AutoBuildingProcessor.autoBuilding("com.ecfront.ez.framework.service.auth")
    Initiator.init()
    Resp.success("")
  }


  override def destroy(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] =
    mutable.Set(com.ecfront.ez.framework.service.jdbc.ServiceAdapter.serviceName)

  override var serviceName: String = "auth"

}


