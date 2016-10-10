package com.ecfront.ez.framework.service.auth

/**
  * 账号 VO
  *
  * 用于显示或添加、更新账号信息
  */
case class AccountVO() {
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
  var ext_info: String = _
}