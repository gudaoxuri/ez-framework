package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.core.rpc.AutoBuildingProcessor
import com.ecfront.ez.framework.service.auth.model._
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._
import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  val EB_ORG_ADD_FLAG = "/ez/auth/rbac/organization/add/"
  val EB_ORG_REMOVE_FLAG = "/ez/auth/rbac/organization/remove/"
  val EB_RESOURCE_ADD_FLAG = "/ez/auth/rbac/resource/add/"
  val EB_RESOURCE_REMOVE_FLAG = "/ez/auth/rbac/resource/remove/"
  val EB_ROLE_ADD_FLAG = "/ez/auth/rbac/role/add/"
  val EB_ROLE_REMOVE_FLAG = "/ez/auth/rbac/role/remove/"

  val EB_ORG_INIT_FLAG = "/ez/auth/organizationInit/"
  val EB_LOGIN_SUCCESS_FLAG = "/ez/auth/loginSuccess/"
  val EB_LOGOUT_FLAG = "/ez/auth/logout/"

  val EB_FLUSH_FLAG = "/ez/auth/flush/"

  var customLogin: Boolean = _
  var defaultOrganizationCode: String = _
  var loginKeepSeconds: Int = _
  var loginLimit_showCaptcha: Int = _
  var encrypt_algorithm: String = _
  var encrypt_salt: String = _

  override def init(parameter: JsonObject): Resp[String] = {
    customLogin = parameter.getBoolean("customLogin", false)
    val loginLimit = parameter.getJsonObject("loginLimit", new JsonObject())
    if (loginLimit.containsKey("showCaptcha")) {
      loginLimit_showCaptcha = loginLimit.getInteger("showCaptcha")
    } else {
      loginLimit_showCaptcha = Int.MaxValue
    }
    if (parameter.containsKey("customTables")) {
      parameter.getJsonObject("customTables").foreach {
        item =>
          item.getKey match {
            case "organization" => EZ_Organization.customTableName(item.getValue.asInstanceOf[String])
            case "account" => EZ_Account.customTableName(item.getValue.asInstanceOf[String])
            case "resource" => EZ_Resource.customTableName(item.getValue.asInstanceOf[String])
            case "role" => EZ_Role.customTableName(item.getValue.asInstanceOf[String])
            case "menu" => EZ_Menu.customTableName(item.getValue.asInstanceOf[String])
            case "rel_account_role" => EZ_Account.TABLE_REL_ACCOUNT_ROLE = item.getValue.asInstanceOf[String]
            case "rel_role_resource" => EZ_Role.TABLE_REL_ROLE_RESOURCE = item.getValue.asInstanceOf[String]
            case "rel_menu_role" => EZ_Menu.TABLE_REL_MENU_ROLE = item.getValue.asInstanceOf[String]
          }
      }
    }
    defaultOrganizationCode = parameter.getString("defaultOrganizationCode", "")
    loginKeepSeconds = parameter.getInteger("loginKeepSeconds", 0)
    encrypt_algorithm =
      if (parameter.containsKey("encrypt") && parameter.getJsonObject("encrypt").containsKey("algorithm")) {
        parameter.getJsonObject("encrypt").getString("algorithm")
      } else {
        "SHA-256"
      }
    encrypt_salt =
      if (parameter.containsKey("encrypt") && parameter.getJsonObject("encrypt").containsKey("salt")) {
        parameter.getJsonObject("encrypt").getString("salt")
      } else {
        ""
      }
    AutoBuildingProcessor.autoBuilding("com.ecfront.ez.framework.service.auth")
    Initiator.init()
    Resp.success("")
  }


  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] =
    mutable.Set(com.ecfront.ez.framework.service.jdbc.ServiceAdapter.serviceName)

  override var serviceName: String = "auth"

}


