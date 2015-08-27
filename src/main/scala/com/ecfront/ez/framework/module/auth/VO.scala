package com.ecfront.ez.framework.module.auth

case class Token_Info_VO(token: String, login_id: String, login_name: String, role_ids: Map[String, String], ext_id: String, last_login_time: Long)
