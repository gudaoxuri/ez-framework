package com.ecfront.ez.framework.service.rpc.foundation

import com.ecfront.ez.framework.service.storage.foundation.EZStorageContext

import scala.beans.BeanProperty
import scala.language.implicitConversions

/**
  * RPC上下文
  */
class EZRPCContext {

  // 请求方法
  @BeanProperty var method: String = _
  // 请求对应的模块URI（可能带通配符）
  @BeanProperty var templateUri: String = _
  // 请求的真实URI
  @BeanProperty var realUri: String = _
  // 请求URL中的参数
  @BeanProperty var parameters: Map[String, String] = _
  // 远程IP
  @BeanProperty var remoteIP: String = _
  // 请求的Accept
  @BeanProperty var accept: String = _
  // 请求的ContentType
  @BeanProperty var contentType: String = _

  /**
    * 在某些情况下（如Simple Service中）必须使用显示方法来转换
    * @return
    */
  def toStorageContext: EZStorageContext = {
    EZStorageContext()
  }

}

object EZRPCContext {

  implicit def toStorageContext(rpcContext: EZRPCContext): EZStorageContext = {
    EZStorageContext()
  }

}
