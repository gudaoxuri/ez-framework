package com.asto.ez.framework.scheduler

import com.asto.ez.framework.storage._
import com.asto.ez.framework.storage.jdbc.JDBCBaseStorage
import com.asto.ez.framework.storage.mongo.MongoBaseStorage

import scala.beans.BeanProperty

@Entity("调度任务日志")
case class EZ_Scheduler_Log() extends BaseModel {

  @BeanProperty var scheduler_name: String = _
  @BeanProperty var start_time: Long = _
  @BeanProperty var end_time: Long = _
  @BeanProperty var success: Boolean = _
  @BeanProperty var desc: String = _

}

object JDBC_EZ_Scheduler_Log extends JDBCBaseStorage[EZ_Scheduler_Log]

object Mongo_EZ_Scheduler_Log extends MongoBaseStorage[EZ_Scheduler_Log]





