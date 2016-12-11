package com.ecfront.ez.framework.service.tpsi

import java.util.Date

import com.ecfront.ez.framework.core.rpc.{Label, Require}

import scala.beans.BeanProperty

class TPSIStartVO() {

  @Label("服务名称")
  @Require
  @BeanProperty var service_code: String = _
  @Label("供应商名称")
  @Require
  @BeanProperty var supplier_code: String = _
  @Label("调用主体")
  @Require
  @BeanProperty var invoke_main_body: String = _

}

class TPSIFinishVO() {

  @Label("开始的日志ID")
  @Require
  @BeanProperty var log_id: Long = _
  @Label("是否成功")
  @Require
  @BeanProperty var success: Boolean = _
  @Label("结果描述")
  @Require
  @BeanProperty var message: String = _

}

class TPSIFullInvokeVO() {

  @Label("服务名称")
  @Require
  @BeanProperty var service_code: String = _
  @Label("供应商名称")
  @Require
  @BeanProperty var supplier_code: String = _
  @Label("调用主体")
  @Require
  @BeanProperty var invoke_main_body: String = _
  @Label("开始时间")
  @Require
  @BeanProperty var start_time: Date = _
  @Label("结束时间")
  @Require
  @BeanProperty var end_time: Date = _
  @Label("是否成功")
  @Require
  @BeanProperty var success: Boolean = _
  @Label("结果描述")
  @Require
  @BeanProperty var message: String = _

}


