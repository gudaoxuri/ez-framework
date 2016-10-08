package com.ecfront.ez.framework.core.rpc

import java.util.Date

/**
  * Opt Info VO
  *
  * @param token                token
  * @param accountCode          code
  * @param login_id             登录id
  * @param name                 姓名
  * @param email                email
  * @param image                头像
  * @param organizationCode     组织编码
  * @param organizationName     组织名称
  * @param organizationCategory 组织类型
  * @param roleCodes            角色编码列表
  * @param extInfo              扩展信息
  */
case class OptInfo(
                    token: String,
                    accountCode: String,
                    login_id: String,
                    name: String,
                    email: String,
                    image: String,
                    organizationCode: String,
                    organizationName: String,
                    organizationCategory: String,
                    roleCodes: Set[String],
                    lastLoginTime: Date,
                    extInfo: Map[String, Any])
