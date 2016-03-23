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

}

trait EZ_Scheduler_Base extends SecureStorage[EZ_Scheduler] with StatusStorage[EZ_Scheduler] {

  def findByModule(module: String): Resp[List[EZ_Scheduler]]

}

object EZ_Scheduler_Mongo extends MongoSecureStorage[EZ_Scheduler] with MongoStatusStorage[EZ_Scheduler] with EZ_Scheduler_Base {

  override def findByModule(module: String): Resp[List[EZ_Scheduler]] = {
    findEnabled(s"""{"module":"$module"}""")
  }

}

object EZ_Scheduler_JDBC extends JDBCSecureStorage[EZ_Scheduler] with JDBCStatusStorage[EZ_Scheduler] with EZ_Scheduler_Base {

  override def findByModule(module: String): Resp[List[EZ_Scheduler]] = {
    findEnabled(s"""module = ?""", List(module))
  }

}
