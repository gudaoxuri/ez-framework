package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.JDBCBaseStorage
import com.ecfront.ez.framework.service.storage.mongo.MongoBaseStorage

import scala.beans.BeanProperty

/**
  * 账号实体
  */
@Entity("Ext_Test_Account")
case class Ext_Test_Account() extends BaseModel {

  @BeanProperty var ext1: String = _
  @BeanProperty var ext2: String = _

}

/*object Ext_Test_Account extends MongoBaseStorage[Ext_Test_Account] {

  override def preSave(model: Ext_Test_Account, context: EZStorageContext): Resp[Ext_Test_Account] = {
    logger.debug(model.toString)
    super.preSave(model, context)
  }
}*/

object Ext_Test_Account extends JDBCBaseStorage[Ext_Test_Account] {

  override def preSave(model: Ext_Test_Account, context: EZStorageContext): Resp[Ext_Test_Account] = {
    logger.debug(model.toString)
    super.preSave(model, context)
  }
}









