package com.ecfront.ez.framework.service.auth

/**
  * Token VO
  *
  * @param token             token
  * @param login_id          登录id
  * @param login_name        姓名
  * @param image             头像
  * @param organization_code 组织code
  * @param organization_name 组织名称
  * @param role_info         角色信息
  * @param ext_id            扩展ID
  * @param ext_info          扩展信息
  * @param last_login_time   最后一次登录时间  System.currentTimeMillis
  */
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

/**
  * 账号 VO
  *
  *
  * 用于显示或添加、更新账号信息
  */
case class Account_VO() {
  // 数据库id，不能更改
  var id: String = _
  // 登录id，不能更改
  var login_id: String = _
  // 姓名
  var name: String = _
  // 头像
  var image: String = _
  // Email
  var email: String = _
  // 当前密码，更新时需要验证
  var current_password: String = _
  // 新密码，如果需要更改密码时填写
  var new_password: String = _
  // 扩展ID，不能更改
  var ext_id: String = _
  // 扩展信息
  var ext_info: Map[String, String] = _
}