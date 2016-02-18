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
                          last_login_time: Long)
