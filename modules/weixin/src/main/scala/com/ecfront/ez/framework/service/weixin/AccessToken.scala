package com.ecfront.ez.framework.service.weixin

/**
  *  Access Token
  */
case class AccessToken(
                        // 网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
                        access_token: String,
                        // access_token接口调用凭证超时时间，单位（秒）
                        expires_in: Long,
                        // 用户刷新access_token
                        refresh_token: String,
                        // 用户唯一标识
                        openid: String,
                        //  用户授权的作用域，使用逗号（,）分隔
                        scope: String,
                        unionid: String
                      )
