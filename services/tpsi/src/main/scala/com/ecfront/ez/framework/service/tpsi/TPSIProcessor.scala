package com.ecfront.ez.framework.service.tpsi

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc._

/**
  * TPSI服务
  */
@RPC("/ez/tpsi/", "EZ-TPSI服务", "")
object TPSIProcessor extends Logging {

  /**
    * 开始调用服务
    */
  @POST("start/", "开始调用服务", "", "", "||Long|服务ID,完成时带回此ID")
  def start(parameter: Map[String, String], body: TPSIStartVO): Resp[String] = {
    if (ServiceAdapter.config.isStorage) {
      Resp.success(EZ_TPSI_Log.start(body.service_code, body.supplier_code, body.invoke_main_body).id)
    } else {
      Resp.notImplemented("TPSI服务记录功能未启用")
    }
  }

  /**
    * 完成服务调用
    */
  @PUT("finish/", "完成服务调用", "", "", "")
  def finish(parameter: Map[String, String], body: TPSIFinishVO): Resp[Void] = {
    if (ServiceAdapter.config.isStorage) {
      EZ_TPSI_Log.finishByLogId(body.success, body.message, body.log_id)
      Resp.success(null)
    } else {
      Resp.notImplemented("TPSI服务记录功能未启用")
    }
  }

  /**
    * 记录服务调用(从开始到结束)
    */
  @POST("add/", "记录服务调用,从开始到结束", "", "", "")
  def add(parameter: Map[String, String], body: TPSIFullInvokeVO): Resp[Void] = {
    if (ServiceAdapter.config.isStorage) {
      EZ_TPSI_Log.add(body.service_code, body.supplier_code, body.invoke_main_body,
        body.success, body.message, body.start_time, body.end_time)
      Resp.success(null)
    } else {
      Resp.notImplemented("TPSI服务记录功能未启用")
    }
  }

}