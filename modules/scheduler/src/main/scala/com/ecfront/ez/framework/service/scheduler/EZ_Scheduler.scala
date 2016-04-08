package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.{Ignore, Resp}
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoSecureStorage, MongoStatusStorage}

import scala.beans.BeanProperty

@Entity("调度任务")
case class EZ_Scheduler() extends SecureModel with StatusModel {

  // 调度名称
  @Unique
  @Label("调度名称")
  @BeanProperty var name: String = _
  // 调度周期
  @BeanProperty var cron: String = _
  // 回调执行的类
  @BeanProperty var clazz: String = _
  // 任务参数
  @BeanProperty
  @Ignore var parameters: Map[String, Any] = _
  // 任务参数(Map to JsonString)
  @BeanProperty
  var parameterstr: String = _
  // 使用的模块
  @BeanProperty var module: String = _

}


object EZ_Scheduler extends SecureStorageAdapter[EZ_Scheduler, EZ_Scheduler_Base]
  with StatusStorageAdapter[EZ_Scheduler, EZ_Scheduler_Base] with EZ_Scheduler_Base {

  override protected val storageObj: EZ_Scheduler_Base =
    if (ServiceAdapter.mongoStorage) EZ_Scheduler_Mongo else EZ_Scheduler_JDBC

  override def findByModule(module: String): Resp[List[EZ_Scheduler]] = {
    storageObj.findByModule(module)
  }

  override def existByModule(module: String): Resp[Boolean] = {
    storageObj.existByModule(module)
  }

  override def deleteByModule(module: String): Resp[Void] = storageObj.deleteByModule(module)

  override def enableByName(name: String): Resp[Void] = storageObj.enableByName(name)

  override def getByName(name: String): Resp[EZ_Scheduler] = storageObj.getByName(name)

  override def deleteByName(name: String): Resp[Void] = storageObj.deleteByName(name)

  override def disableByModule(module: String): Resp[Void] = storageObj.disableByModule(module)

  override def disableByName(name: String): Resp[Void] = storageObj.disableByName(name)

  override def enableByModule(module: String): Resp[Void] = storageObj.enableByModule(module)

}

trait EZ_Scheduler_Base extends SecureStorage[EZ_Scheduler] with StatusStorage[EZ_Scheduler] {

  def findByModule(module: String): Resp[List[EZ_Scheduler]]

  def deleteByModule(module: String): Resp[Void]

  def enableByModule(module: String): Resp[Void]

  def disableByModule(module: String): Resp[Void]

  def existByModule(module: String): Resp[Boolean]

  def deleteByName(name: String): Resp[Void]

  def enableByName(name: String): Resp[Void]

  def disableByName(name: String): Resp[Void]

  def getByName(name: String): Resp[EZ_Scheduler]

}

object EZ_Scheduler_Mongo extends MongoSecureStorage[EZ_Scheduler] with MongoStatusStorage[EZ_Scheduler] with EZ_Scheduler_Base {

  override def findByModule(module: String): Resp[List[EZ_Scheduler]] = {
    findEnabled(s"""{"module":"$module"}""")
  }

  override def existByModule(module: String): Resp[Boolean] = {
    existEnabledByCond(s"""{"module":"$module"}""")
  }

  override def deleteByModule(module: String): Resp[Void] = {
    deleteByCond(s"""{"module":"$module"}""")
  }

  override def enableByModule(module: String): Resp[Void] = {
    updateByCond(s"""{"enable":true}""",s"""{"module":"$module"}""")
  }

  override def disableByModule(module: String): Resp[Void] = {
    updateByCond(s"""{"enable":false}""",s"""{"module":"$module"}""")
  }

  override def enableByName(name: String): Resp[Void] = {
    updateByCond(s"""{"enable":true}""",s"""{"name":"$name"}""")
  }

  override def disableByName(name: String): Resp[Void] = {
    updateByCond(s"""{"enable":false}""",s"""{"name":"$name"}""")
  }

  override def getByName(name: String): Resp[EZ_Scheduler] = {
    getByCond(s"""{"name":"$name"}""")
  }

  override def deleteByName(name: String): Resp[Void] = {
    deleteByCond(s"""{"name":"$name"}""")
  }

}

object EZ_Scheduler_JDBC extends JDBCSecureStorage[EZ_Scheduler] with JDBCStatusStorage[EZ_Scheduler] with EZ_Scheduler_Base {

  override def findByModule(module: String): Resp[List[EZ_Scheduler]] = {
    findEnabled(s"""module = ?""", List(module))
  }

  override def existByModule(module: String): Resp[Boolean] = {
    existEnabledByCond(s"""module = ?""", List(module))
  }

  override def deleteByModule(module: String): Resp[Void] = {
    deleteByCond(s"""module = ?""", List(module))
  }

  override def enableByModule(module: String): Resp[Void] = {
    updateByCond(s"""enable = true """,s""" module = ? """, List(module))
  }

  override def disableByModule(module: String): Resp[Void] = {
    updateByCond(s"""enable = false """,s""" module = ? """, List(module))
  }

  override def enableByName(name: String): Resp[Void] = {
    updateByCond(s"""enable = true """,s""" name = ? """, List(name))
  }

  override def disableByName(name: String): Resp[Void] = {
    updateByCond(s"""enable = false """,s""" name = ? """, List(name))
  }

  override def getByName(name: String): Resp[EZ_Scheduler] = {
    getByCond(s""" name = ? """, List(name))
  }

  override def deleteByName(name: String): Resp[Void] = {
    deleteByCond(s""" name = ? """, List(name))
  }

}
