package com.ecfront.ez.framework.service.oauth2

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    OAuth2Service.init(parameter)
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: collection.mutable.Set[String] = collection.mutable.Set(
    com.ecfront.ez.framework.service.auth.ServiceAdapter.serviceName
  )

}


