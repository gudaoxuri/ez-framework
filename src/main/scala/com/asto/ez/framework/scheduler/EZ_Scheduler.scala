package com.asto.ez.framework.scheduler

import com.asto.ez.framework.storage.jdbc._
import com.ecfront.common.Ignore

import scala.beans.BeanProperty

@Entity("调度任务")
case class EZ_Scheduler() extends JDBCSecureModel with JDBCStatusModel {

  @Id("seq")
  @BeanProperty
  var id: Long = _
  //调度名称
  @BeanProperty var name: String = _
  //调度周期
  @BeanProperty var cron: String = _
  //回调执行的类
  @BeanProperty var clazz: String = _
  //任务参数
  @BeanProperty @Ignore var parameters: Map[String,Any] = _
  //任务参数(Map to JsonString)
  @BeanProperty @Text var parameterstr: String = _
  //使用的模块
  @BeanProperty var module: String = _
}
