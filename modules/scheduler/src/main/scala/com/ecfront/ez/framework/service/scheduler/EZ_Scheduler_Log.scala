package com.ecfront.ez.framework.service.scheduler

import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.JDBCBaseStorage
import com.ecfront.ez.framework.service.storage.mongo.MongoBaseStorage

import scala.beans.BeanProperty

@Entity("调度任务日志")
case class EZ_Scheduler_Log() extends BaseModel {

  @Label("调度名称")
  @BeanProperty var scheduler_name: String = _
  @Label("开始时间")
  @BeanProperty var start_time: Long = _
  @Label("结束时间")
  @BeanProperty var end_time: Long = _
  @Label("是否成功")
  @BeanProperty var success: Boolean = _
  @Label("结果描述")
  @BeanProperty var message: String = _

}

object EZ_Scheduler_Log extends BaseStorageAdapter[EZ_Scheduler_Log, BaseStorage[EZ_Scheduler_Log]] {
  override protected val storageObj: BaseStorage[EZ_Scheduler_Log] =
    if (ServiceAdapter.mongoStorage) EZ_Scheduler_Log_Mongo else EZ_Scheduler_Log_JDBC
}

object EZ_Scheduler_Log_JDBC extends JDBCBaseStorage[EZ_Scheduler_Log]

object EZ_Scheduler_Log_Mongo extends MongoBaseStorage[EZ_Scheduler_Log]





