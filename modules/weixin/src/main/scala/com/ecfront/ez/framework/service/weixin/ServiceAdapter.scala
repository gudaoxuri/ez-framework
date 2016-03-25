package com.ecfront.ez.framework.service.weixin

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import com.ecfront.ez.framework.service.rpc.http.HTTP
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  var appId:String=_
  var secret:String=_
  var messageToken:String=_

  override def init(parameter: JsonObject): Resp[String] = {
    appId=parameter.getString("appId")
    secret=parameter.getString("secret")
    messageToken=parameter.getString("messageToken")
    AutoBuildingProcessor.autoBuilding[HTTP]("com.ecfront.ez.framework.service.weixin.api", classOf[HTTP])
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: collection.mutable.Set[String] = collection.mutable.Set(
    com.ecfront.ez.framework.service.oauth2.ServiceAdapter.serviceName,
    com.ecfront.ez.framework.service.redis.ServiceAdapter.serviceName
  )

  override var serviceName: String = "weixin"

}


