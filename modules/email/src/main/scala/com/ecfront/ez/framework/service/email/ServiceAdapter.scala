package com.ecfront.ez.framework.service.email

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import io.vertx.core.json.JsonObject
import io.vertx.ext.mail.MailConfig

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    EmailProcessor.init(new MailConfig(parameter))
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

}


