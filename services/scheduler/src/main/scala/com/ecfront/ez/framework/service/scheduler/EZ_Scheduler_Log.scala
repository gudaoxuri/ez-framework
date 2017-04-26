package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

@Entity("调度任务日志")
case class EZ_Scheduler_Log() extends BaseModel {

  @Index
  @Desc("调度名称",200,0)
  @BeanProperty var scheduler_name: String = _
  @Desc("开始时间",0,0)
  @BeanProperty var start_time: Long = _
  @Desc("结束时间",0,0)
  @BeanProperty var end_time: Long = _
  @Desc("是否成功",0,0)
  @BeanProperty var success: Boolean = _
  @Desc("结果描述",500,0)
  @BeanProperty var message: String = _

}

object EZ_Scheduler_Log extends BaseStorage[EZ_Scheduler_Log] {

  def pageByName(name: String, pageNumber: Long, pageSize: Int): Resp[Page[EZ_Scheduler_Log]] = {
    page("scheduler_name = ?", List(name), pageNumber, pageSize)
  }

}




