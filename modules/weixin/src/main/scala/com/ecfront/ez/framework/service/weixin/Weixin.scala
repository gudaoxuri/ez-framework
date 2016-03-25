package com.ecfront.ez.framework.service.weixin

import com.ecfront.ez.framework.service.weixin.module.MessageService

object Weixin {

  def init(_messageService: MessageService): Unit = {
    messageService = _messageService
  }

  private[weixin] var messageService: MessageService = null

}
