package com.ecfront.ez.framework.service.weixin.vo

import org.jsoup.nodes.Document

abstract class BaseRecMessageVO extends Serializable {
  // 开发者微信号
  var toUserName: String = _
  // 发送方帐号（一个OpenID）
  var fromUserName: String = _
  // 消息创建时间 （整型）
  var createTime: String = _
  // 消息类型
  var msgType: String = _
}

object BaseRecMessageVO {
  def apply[E <: BaseRecMessageVO](vo: E, doc: Document): E = {
    vo.toUserName = doc.getElementsByTag("ToUserName").text()
    vo.fromUserName = doc.getElementsByTag("FromUserName").text()
    vo.createTime = doc.getElementsByTag("CreateTime").text()
    vo.msgType = doc.getElementsByTag("MsgType").text()
    vo
  }
}

abstract class BaseNormalRecMsgVO extends BaseRecMessageVO {
  // 消息id，64位整型
  var msgId: String = _
}

object BaseNormalRecMsgVO {
  def apply[E <: BaseNormalRecMsgVO](vo: E, doc: Document): E = {
    BaseRecMessageVO(vo, doc)
    vo.msgId = doc.getElementsByTag("MsgId").text()
    vo
  }
}

case class TextRecMsgVO() extends BaseNormalRecMsgVO {
  // 文本消息内容
  var content: String = _
}

object TextRecMsgVO {
  def apply(doc: Document): TextRecMsgVO = {
    val vo = new TextRecMsgVO()
    BaseNormalRecMsgVO(vo, doc)
    vo.content = doc.getElementsByTag("Content").text()
    vo
  }
}

class ImageRecMsgVO extends BaseNormalRecMsgVO {
  // 图片链接
  var picUrl: String = _
  // 图片消息媒体id，可以调用多媒体文件下载接口拉取数据
  var mediaId: String = _
}

class ImageSendMsgVO extends BaseNormalRecMsgVO {
  // 通过素材管理接口上传多媒体文件，得到的id
  var mediaId: String = _
}

class VoiceRecMsgVO extends BaseNormalRecMsgVO {
  // 语音消息媒体id，可以调用多媒体文件下载接口拉取数据
  var mediaId: String = _
  // 语音格式，如amr，speex等
  var format: String = _
  /**
    * 开通语音识别后，用户每次发送语音给公众号时，
    * 微信会在推送的语音消息XML数据包中，增加一个Recongnition字段
    * （注：由于客户端缓存，开发者开启或者关闭语音识别功能，对新关注者立刻生效，对已关注用户需要24小时生效。开发者可以重新关注此帐号进行测试）
    */
  var recognition: String = _
}

class VoiceReplyMsgVO extends BaseNormalRecMsgVO {
  //  通过素材管理接口上传多媒体文件，得到的id
  var mediaId: String = _
}

class VideoRecMsgVO extends BaseNormalRecMsgVO {
  // 视频消息媒体id，可以调用多媒体文件下载接口拉取数据
  var mediaId: String = _
  // 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据
  var thumbMediaId: String = _
}

class ShortVideoRecMsgVO extends BaseNormalRecMsgVO {
  // 视频消息媒体id，可以调用多媒体文件下载接口拉取数据
  var mediaId: String = _
  // 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据
  var thumbMediaId: String = _
}

class VideoReplyMsgVO extends BaseNormalRecMsgVO {
  //  通过素材管理接口上传多媒体文件，得到的id
  var mediaId: String = _
  // 视频消息的标题（可选）
  var title: String = _
  // 视频消息的描述（可选）
  var description: String = _
}

class LocationRecMsgVO extends BaseNormalRecMsgVO {
  var locationX: String = _
  // 地理位置维度
  var locationY: String = _
  // 地理位置经度
  var scale: String = _
  // 地图缩放大小
  var label: String = _ // 地理位置信息
}

class LinkRecMsgVO extends BaseNormalRecMsgVO {
  var title: String = _
  // 消息标题
  var description: String = _
  // 消息描述
  var url: String = _ // 消息链接
}

class MusicReplyMsgVO extends BaseNormalRecMsgVO {
  // 音乐的标题（可选）
  var title: String = _
  // 音乐的描述（可选）
  var description: String = _
  //  音乐链接（可选）
  var musicURL: String = _
  //  高质量音乐链接，WIFI环境优先使用该链接播放音乐（可选）
  var hQMusicUrl: String = _
  //   缩略图的媒体id，通过素材管理接口上传多媒体文件，得到的id  （可选）
  var thumbMediaId: String = _
}

class NewsReplyMsgVO extends BaseNormalRecMsgVO {
  //   图文消息个数，限制为10条以内
  var articleCount: Int = _
  //  多条图文消息信息，默认第一个item为大图,注意，如果图文数超过10，则将会无响应
  var articles: String = _
  // 音乐的标题（可选）
  var title: String = _
  // 音乐的描述（可选）
  var description: String = _
  //   图片链接，支持JPG、PNG格式，较好的效果为大图360*200，小图200*200（可选）
  var picUrl: String = _
  //   点击图文消息跳转链接 （可选）
  var url: String = _
}

class BaseEventRecMsgVO extends BaseRecMessageVO {
  // 事件类型
  var event: String = _
}

class SubscribeEventRecMsgVO extends BaseEventRecMsgVO

class UnSubscribeEventRecMsgVO extends BaseEventRecMsgVO

class QRUnFollowEventRecMsgVO extends BaseEventRecMsgVO {
  // 事件KEY值，qrscene_为前缀，后面为二维码的参数值
  var eventKey: String = _
  // 二维码的ticket，可用来换取二维码图片
  var ticket: String = _
}

class QRFollowedEventRecMsgVO extends BaseEventRecMsgVO {
  // 事件KEY值，是一个32位无符号整数，即创建二维码时的二维码scene_id
  var eventKey: String = _
  // 二维码的ticket，可用来换取二维码图片
  var ticket: String = _
}

class LocationEventRecMsgVO extends BaseEventRecMsgVO {
  // 地理位置维度
  var latitude: String = _
  // 地理位置经度
  var longitude: String = _
  //  地理位置精度
  var precision: String = _
}

class ClickEventRecMsgVO extends BaseEventRecMsgVO {
  // 事件KEY值，与自定义菜单接口中KEY值对应
  var eventKey: String = _
}

class ViewEventRecMsgVO extends BaseEventRecMsgVO {
  //  事件KEY值，设置的跳转URL
  var eventKey: String = _
}