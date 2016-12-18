package com.ecfront.ez.framework.service.tpsi

import java.util.Date

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

@Entity("执行日志")
case class EZ_TPSI_Log() extends BaseModel {

  @Index
  @Desc("服务名称", 200, 0)
  @BeanProperty var service_code: String = _
  @Index
  @Desc("供应商名称", 200, 0)
  @BeanProperty var supplier_code: String = _
  @Index
  @Desc("调用主体", 200, 0)
  @BeanProperty var invoke_main_body: String = _
  @Desc("开始年份", 0, 0)
  @BeanProperty var start_year: Long = _
  @Desc("开始月份", 0, 0)
  @BeanProperty var start_month: Long = _
  @Desc("开始时间", 0, 0)
  @BeanProperty var start_time: Long = _
  @Desc("结束时间", 0, 0)
  @BeanProperty var end_time: Long = _
  @Desc("执行时间", 0, 0)
  @BeanProperty var use_time: Long = _
  @Desc("是否成功", 0, 0)
  @BeanProperty var success: Boolean = _
  @Desc("结果描述", 500, 0)
  @BeanProperty var message: String = _

}

object EZ_TPSI_Log extends BaseStorage[EZ_TPSI_Log] {

  def add(serviceCode: String, supplierCode: String, invokeMainBody: String,
          success: Boolean, message: String, startTime: Date, endTime: Date): Unit = {
    val log = new EZ_TPSI_Log
    log.service_code = serviceCode
    log.supplier_code = supplierCode
    log.invoke_main_body = invokeMainBody
    log.start_year = TimeHelper.yf.format(startTime).toLong
    log.start_month = TimeHelper.Mf.format(startTime).toLong
    log.start_time = TimeHelper.msf.format(startTime).toLong
    log.end_time = TimeHelper.msf.format(endTime).toLong
    log.use_time = log.end_time - log.start_time
    log.success = success
    log.message = message
    addEvent(EZ_TPSI_Log.save(log).body)
  }

  def start(serviceCode: String, supplierCode: String, invokeMainBody: String = ""): EZ_TPSI_Log = {
    val now = new Date()
    val log = new EZ_TPSI_Log
    log.service_code = serviceCode
    log.supplier_code = supplierCode
    log.invoke_main_body = invokeMainBody
    log.start_year = TimeHelper.yf.format(now).toLong
    log.start_month = TimeHelper.Mf.format(now).toLong
    log.start_time = TimeHelper.msf.format(now).toLong
    EZ_TPSI_Log.save(log).body
  }

  def finishByLogId(success: Boolean, message: String, logId: Long, endTime: Date = new Date()): Unit = {
    val log = EZ_TPSI_Log.getById(logId).body
    if (log != null) {
      log.end_time = TimeHelper.msf.format(endTime).toLong
      log.use_time = log.end_time - log.start_time
      log.success = success
      log.message = message
      addEvent(EZ_TPSI_Log.update(log).body)
    }
  }

  def finish(success: Boolean, message: String, log: EZ_TPSI_Log, endTime: Date = new Date()): Unit = {
    log.end_time = TimeHelper.msf.format(endTime).toLong
    log.use_time = log.end_time - log.start_time
    log.success = success
    log.message = message
    addEvent(EZ_TPSI_Log.update(log).body)
  }

  def pageByCode(serviceCode: String, supplierCode: String, pageNumber: Long, pageSize: Int): Resp[Page[EZ_TPSI_Log]] = {
    page("service_code = ? and supplier_code = ? order by desc", List(serviceCode, supplierCode), pageNumber, pageSize)
  }

  def addEvent(log: EZ_TPSI_Log): Unit = {
    EZ.eb.pubReq(ServiceAdapter.TPSI_ADD_FLAG, log)
  }

}




