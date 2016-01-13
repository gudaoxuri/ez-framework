package com.asto.ez.framework.scheduler

import com.asto.ez.framework.storage._
import com.asto.ez.framework.storage.jdbc._
import com.asto.ez.framework.storage.mongo.{MongoStatusStorage, MongoSecureStorage}
import com.ecfront.common.Ignore

import scala.beans.BeanProperty

@Entity("调度任务")
case class EZ_Scheduler() extends SecureModel with StatusModel {

  //调度名称
  @Unique @Label("调度名称")
  @BeanProperty var name: String = _
  //调度周期
  @BeanProperty var cron: String = _
  //回调执行的类
  @BeanProperty var clazz: String = _
  //任务参数
  @BeanProperty
  @Ignore var parameters: Map[String, Any] = _
  //任务参数(Map to JsonString)
  @BeanProperty
  @Text var parameterstr: String = _
  //使用的模块
  @BeanProperty var module: String = _

}

object JDBC_EZ_Scheduler extends JDBCSecureStorage[EZ_Scheduler] with JDBCStatusStorage[EZ_Scheduler]

object Mongo_EZ_Scheduler extends MongoSecureStorage[EZ_Scheduler] with MongoStatusStorage[EZ_Scheduler]
