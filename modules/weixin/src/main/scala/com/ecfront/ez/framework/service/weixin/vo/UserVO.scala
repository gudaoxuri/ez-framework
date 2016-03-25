package com.ecfront.ez.framework.service.weixin.vo

class UserVO extends Serializable {
  // 用户的标识，对当前公众号唯一
  var openid: String = _
  // 用户的昵称
  var nickname: String = _
  // 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
  var sex: Int = _
  // 用户所在城市
  var city: String = _
  // 用户所在省份
  var province: String = _
  // 用户所在国家
  var country: String = _
  // 用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空。若用户更换头像，原有头像URL将失效。
  var headimgurl: String = _
  // 用户关注时间，为时间戳。如果用户曾多次关注，则取最后关注时间
  var subscribe_time: Long = _
  // 只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段
  var unionid: String = _
  // 公众号运营者对粉丝的备注，公众号运营者可在微信公众平台用户管理界面对粉丝添加备注
  var remark: String = _
  // 用户所在的分组ID
  var groupid: Int = _
}
