package com.ecfront.ez.framework.service.oauth2

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import com.ecfront.ez.framework.service.rpc.http.HTTP
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    AutoBuildingProcessor.autoBuilding[HTTP]("com.ecfront.ez.framework.service.oauth2", classOf[HTTP])
    OAuth2Service.init(parameter)
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: collection.mutable.Set[String] = collection.mutable.Set(
    com.ecfront.ez.framework.service.auth.ServiceAdapter.serviceName
  )

  override var serviceName: String = "oauth2"

}


