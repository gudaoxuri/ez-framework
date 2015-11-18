package com.asto.ez.framework.rpc

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