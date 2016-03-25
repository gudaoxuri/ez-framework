package com.ecfront.ez.framework.service.weixin.vo

class BaseMessageVO extends Serializable {
  var toUserName: String = _
  // 开发者微信号
  var fromUserName: String = _
  // 发送方帐号（一个OpenID）
  var createTime: String = _
  // 消息创建时间 （整型）
  var msgType: String = _ // 消息类型
}

class BaseNormalMessageVO extends BaseMessageVO {
  var msgId: String = _ // 消息id，64位整型
}

class TextNormalMessageVO extends BaseNormalMessageVO {
  var content: String = _ // 文本消息内容
}

class ImageNormalMessageVO extends BaseNormalMessageVO {
  var picUrl: String = _
  // 图片链接
  var mediaId: String = _ // 图片消息媒体id，可以调用多媒体文件下载接口拉取数据
}

class VoiceNormalMessageVO extends BaseNormalMessageVO {
  var mediaId: String = _
  // 语音消息媒体id，可以调用多媒体文件下载接口拉取数据
  var format: String = _ // 语音格式，如amr，speex等
  /**
    * 开通语音识别后，用户每次发送语音给公众号时，
    * 微信会在推送的语音消息XML数据包中，增加一个Recongnition字段
    * （注：由于客户端缓存，开发者开启或者关闭语音识别功能，对新关注者立刻生效，对已关注用户需要24小时生效。开发者可以重新关注此帐号进行测试）
    */
  var recognition: String = _
}

class VideoNormalMessageVO extends BaseNormalMessageVO {
  var mediaId: String = _
  // 视频消息媒体id，可以调用多媒体文件下载接口拉取数据
  var thumbMediaId: String = _ // 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据
}

class LocationNormalMessageVO extends BaseNormalMessageVO {
  var locationX: String = _
  // 地理位置维度
  var locationY: String = _
  // 地理位置经度
  var scale: String = _
  // 地图缩放大小
  var label: String = _ // 地理位置信息
}

class LinkNormalMessageVO extends BaseNormalMessageVO {
  var title: String = _
  // 消息标题
  var description: String = _
  // 消息描述
  var url: String = _ // 消息链接
}

class BaseEventMessageVO extends BaseMessageVO {
  var event: String = _ // 事件类型
}

class SubscribeEventMessageVO extends BaseEventMessageVO

class UnSubscribeEventMessageVO extends BaseEventMessageVO

class QRUnFollowEventMessageVO extends BaseEventMessageVO{
  var eventKey : String = _ // 事件KEY值，qrscene_为前缀，后面为二维码的参数值
  var ticket : String = _ // 二维码的ticket，可用来换取二维码图片
}

class QRFollowedEventMessageVO extends BaseEventMessageVO{
  var eventKey : String = _ // 事件KEY值，是一个32位无符号整数，即创建二维码时的二维码scene_id
  var ticket : String = _ // 二维码的ticket，可用来换取二维码图片
}

class LocationEventMessageVO extends BaseEventMessageVO{
  // 地理位置维度
  var latitude: String = _
  // 地理位置经度
  var longitude: String = _
  //  地理位置精度
  var precision : String = _
}

class ClickEventMessageVO extends BaseEventMessageVO {
  var eventKey : String = _ // 事件KEY值，与自定义菜单接口中KEY值对应
}

class ViewEventMessageVO extends BaseEventMessageVO {
  var eventKey : String = _ //  事件KEY值，设置的跳转URL
}