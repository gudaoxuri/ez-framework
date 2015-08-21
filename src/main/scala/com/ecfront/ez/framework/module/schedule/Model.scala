package com.ecfront.ez.framework.module.schedule

import com.ecfront.ez.framework.service.IdModel
import com.ecfront.storage.{Entity, Index}

import scala.beans.BeanProperty

@Entity("EZ Schedule Tasks")
case class EZ_Schedule_Task() extends IdModel {
  @BeanProperty
  @Index var task_name: String = _
  @BeanProperty
  @Index var module_name: String = _
  @BeanProperty var task_path: String = _
  @BeanProperty var parameters: String = _
  @BeanProperty var delay: Long = _
  @BeanProperty var period: Long = _
  @BeanProperty
  @Index var last_execute_success_time: Long = _
  @BeanProperty var last_execute_finish_time: Long = _
  @BeanProperty var last_execute_finish_message: String = _
  @BeanProperty var is_enabled: Boolean = _
}

@Entity("EZ Schedule Logs")
case class EZ_Schedule_Log() extends IdModel {
  @BeanProperty
  @Index var task_id: String = _
  @BeanProperty
  @Index var task_name: String = _
  @BeanProperty
  @Index var module_name: String = _
  @BeanProperty var execute_start_time: Long = _
  @BeanProperty var execute_finish_time: Long = _
  @BeanProperty var execute_finish_message: String = _
  @BeanProperty var is_successful: Boolean = _
}




