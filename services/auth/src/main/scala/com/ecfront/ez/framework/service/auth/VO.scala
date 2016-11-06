package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.core.rpc.{Label, Require}

/**
  * 账号 VO
  *
  * 用于显示或添加、更新账号信息
  */
case class AccountVO() {
  @Label("数据库id，不能更改")
  @Require
  var id: String = _
  @Label("登录id，不能更改")
  @Require
  var login_id: String = _
  @Label("姓名")
  @Require
  var name: String = _
  @Label("头像")
  @Require
  var image: String = _
  @Label("Email")
  @Require
  var email: String = _
  @Label("当前密码，更新时需要验证")
  @Require
  var current_password: String = _
  @Label("新密码，如果需要更改密码时填写")
  var new_password: String = _
  @Label("组织编码，不能更改")
  var organization_code: String = _
  @Label("扩展ID，不能更改")
  var ext_id: String = _
  @Label("扩展信息")
  var ext_info: String = _
}