package com.asto.ez.framework.auth

case class Token_Info_VO(
                          token: String,
                          login_id: String,
                          login_name: String,
                          image: String,
                          organization_code: String,
                          organization_name: String,
                          role_info: Map[String, String],
                          ext_id: String,
                          ext_info: Map[String, String],
                          last_login_time: Long)

case class Account_VO() {
  var id: String = _
  var login_id: String = _
  var name: String = _
  var image: String = _
  var email: String = _
  var old_password: String = _
  var new_password: String = _
  var ext_id: String = _
  var ext_info: Map[String,String] = _
}