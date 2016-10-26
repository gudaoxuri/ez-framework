package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.{Ignore, Resp}
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

@Entity("调度任务")
case class EZ_Scheduler() extends SecureModel with StatusModel {

  @Unique
  @Desc("调度名称", 200, 0)
  @BeanProperty var name: String = _
  @Desc("调度周期", 50, 0)
  @BeanProperty var cron: String = _
  @Desc("回调执行的类", 500, 0)
  @BeanProperty var clazz: String = _
  // 任务参数
  @BeanProperty
  @Ignore var parameters: Map[String, Any] = _
  @Desc("任务参数", 0, 0)
  @BeanProperty
  var parameterstr: String = _
  @Desc("使用的模块", 200, 0)
  @BeanProperty var module: String = _

}

object EZ_Scheduler extends SecureStorage[EZ_Scheduler] with StatusStorage[EZ_Scheduler] {

  def findByModule(module: String): Resp[List[EZ_Scheduler]] = {
    findEnabled(s"""module = ?""", List(module))
  }

  def existByModule(module: String): Resp[Boolean] = {
    existEnabledByCond(s"""module = ?""", List(module))
  }

  def deleteByModule(module: String): Resp[Void] = {
    deleteByCond(s"""module = ?""", List(module))
  }

  def enableByModule(module: String): Resp[Void] = {
    updateByCond(s"""enable = true """,s""" module = ? """, List(module))
  }

  def disableByModule(module: String): Resp[Void] = {
    updateByCond(s"""enable = false """,s""" module = ? """, List(module))
  }

  def enableByName(name: String): Resp[Void] = {
    updateByCond(s"""enable = true """,s""" name = ? """, List(name))
  }

  def disableByName(name: String): Resp[Void] = {
    updateByCond(s"""enable = false """,s""" name = ? """, List(name))
  }

  def getByName(name: String): Resp[EZ_Scheduler] = {
    getByCond(s""" name = ? """, List(name))
  }

  def deleteByName(name: String): Resp[Void] = {
    deleteByCond(s""" name = ? """, List(name))
  }

}
