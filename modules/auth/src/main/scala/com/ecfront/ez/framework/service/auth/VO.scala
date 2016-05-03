package com.ecfront.ez.framework.service.auth

/**
  * Token VO
  *
  * @param token             token
  * @param account_code      code
  * @param login_id          登录id
  * @param name              姓名
  * @param email             email
  * @param image             头像
  * @param organization_code 组织编码
  * @param role_codes        角色编码列表
  * @param ext_id            扩展ID
  * @param ext_info          扩展信息
  */
case class Token_Info_VO(
                          token: String,
                          account_code: String,
                          login_id: String,
                          name: String,
                          email: String,
                          image: String,
                          organization_code: String,
                          role_codes: List[String],
                          ext_id: String,
                          ext_info: Map[String, Any])

/**
  * 账号 VO
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
  // 组织编码，不能更改
  var organization_code: String = _
  // 扩展ID，不能更改
  var ext_id: String = _
  // 扩展信息
  var ext_info: Map[String, Any] = _
}