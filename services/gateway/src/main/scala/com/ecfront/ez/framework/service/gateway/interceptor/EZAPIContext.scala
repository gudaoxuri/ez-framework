package com.ecfront.ez.framework.service.gateway.interceptor

import com.ecfront.ez.framework.core.rpc.OptInfo

import scala.beans.BeanProperty
import scala.language.implicitConversions

/**
  * RPC上下文
  */
class EZAPIContext {

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
  // Token
  @BeanProperty var token: Option[String] = None
  // 认证信息
  @BeanProperty var optInfo: Option[OptInfo] = None
  // 处理结果
  @BeanProperty var executeResult: Any = _

}
