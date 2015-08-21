package com.ecfront.ez.framework.rpc

import scala.annotation.StaticAnnotation

/**
 * RPC服务入口
 */
object RPC {

  System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")

  /**
   * 创建一个服务器
   */
  def server = Server()

  /**
   * 创建一个连接客户端
   */
  def client = Client()

  /**
   * 服务通道
   * <p>
   * 支持 HTTP、Web Sockets 、 Event Bus
   * <ul>
   * <li>HTTP : 用于常规的HTTP服务，不支持Session、Cookies及HTTP Header定义</li>
   * <li>Web Sockets : 用于需要保持长连接的HTTP服务</li>
   * <li>Event Bus : 用于高性能的节点间通讯，支持集群负载均衡，支持publish subscribe 及 point to point </li>
   * </ul>
   */
  object EChannel extends Enumeration {
    type EChannel = Value
    val HTTP, EVENT_BUS, WEB_SOCKETS = Value
  }

}

//======================Auto Building Annotations======================

/**
 * 标记需要注册RPC的类，用于Auto Building特性
 * @param baseUri 此类的根路径
 */
case class RPC(baseUri: String) extends StaticAnnotation

/**
 * 注册用于接收Get请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class GET(uri: String) extends StaticAnnotation

/**
 * 注册用于接收Post请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class POST(uri: String) extends StaticAnnotation

/**
 * 注册用于接收Put请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class PUT(uri: String) extends StaticAnnotation

/**
 * 注册用于接收Delete请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class DELETE(uri: String) extends StaticAnnotation

/**
 * 标记使用HTTP通道，用于Auto Building特性
 * <p>
 * 如写在类中表明此类下所有RPC方法都支持HTTP通道，如写在某个方法中表示此方法支持HTTP通道
 */
class HTTP extends StaticAnnotation

/**
 * 标记使用Web Sockets通道，用于Auto Building特性
 * <p>
 * 如写在类中表明此类下所有RPC方法都支持Web Sockets通道，如写在某个方法中表示此方法支持Web Sockets通道
 */
class WEB_SOCKETS extends StaticAnnotation

/**
 * 标记使用Event Bus通道，用于Auto Building特性
 * <p>
 * 如写在类中表明此类下所有RPC方法都支持Event Bus通道，如写在某个方法中表示此方法支持Event Bus通道
 */
class EVENT_BUS extends StaticAnnotation
