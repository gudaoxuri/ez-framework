package com.ecfront.ez.framework.service.weixin.module

import com.ecfront.ez.framework.service.weixin.vo.{BaseReplyMessageVO, TextRecMsgVO}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait MessageService extends LazyLogging {

  def processTextMsg(message: TextRecMsgVO): BaseReplyMessageVO

}
