package com.ecfront.ez.framework.service.weixin

import com.ecfront.ez.framework.service.weixin.module.MessageService
import com.ecfront.ez.framework.service.weixin.vo.{BaseReplyMessageVO, TextRecMsgVO, TextReplyMsgVO}

object MockMessageService extends MessageService {

  override def processTextMsg(message: TextRecMsgVO): BaseReplyMessageVO = {
    val vo = TextReplyMsgVO()
    vo.content = message.content
    vo
  }

}
