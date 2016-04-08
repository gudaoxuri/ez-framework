package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.Resp
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

object EZ_Scheduler_Log extends BaseStorageAdapter[EZ_Scheduler_Log, EZ_Scheduler_Log_Base] with EZ_Scheduler_Log_Base {

  override protected val storageObj: EZ_Scheduler_Log_Base =
    if (ServiceAdapter.mongoStorage) EZ_Scheduler_Log_Mongo else EZ_Scheduler_Log_JDBC

  override def pageByName(name: String, pageNumber: Long, pageSize: Int): Resp[Page[EZ_Scheduler_Log]] =
    storageObj.pageByName(name, pageNumber, pageSize)
}

trait EZ_Scheduler_Log_Base extends BaseStorage[EZ_Scheduler_Log] {

  def pageByName(name: String, pageNumber: Long, pageSize: Int): Resp[Page[EZ_Scheduler_Log]]

}

object EZ_Scheduler_Log_JDBC extends JDBCBaseStorage[EZ_Scheduler_Log] with EZ_Scheduler_Log_Base {

  override def pageByName(name: String, pageNumber: Long, pageSize: Int): Resp[Page[EZ_Scheduler_Log]] = {
    page("scheduler_name = ?", List(name), pageNumber, pageSize)
  }

}

object EZ_Scheduler_Log_Mongo extends MongoBaseStorage[EZ_Scheduler_Log] with EZ_Scheduler_Log_Base {

  override def pageByName(name: String, pageNumber: Long, pageSize: Int): Resp[Page[EZ_Scheduler_Log]] = {
    page(s"""{"scheduler_name":"$name"}""", List(), pageNumber, pageSize)
  }

}





