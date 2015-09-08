package com.ecfront.ez.framework.rpc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.rpc.RPC.EChannel
import com.ecfront.ez.framework.rpc.RPC.EChannel._
import com.ecfront.ez.framework.rpc.process.ClientProcessor
import com.ecfront.ez.framework.rpc.process.channel.eb.EventBusClientProcessor
import com.ecfront.ez.framework.rpc.process.channel.http.HttpClientProcessor
import com.ecfront.ez.framework.rpc.process.channel.websockets.WebSocketsClientProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * 连接客户端
 * <p>
 * 支持标准的基于Json的Restful风格，返回结果为统一的Result或自定义对象
 */
case class Client() extends LazyLogging {

  private val client = this
  /**
   * 服务连接的端口（对Event Bus通道无效）
   */
  private var port = 8080
  /**
   * 服务连接的IP（对Event Bus通道无效）
   */
  private var host = "0.0.0.0"
  /**
   * 后置接收方法，可用于权限过滤
   * <p>
   * 当返回 code=200 时表示执行成功，进入业务方法，反之表示执行出错，直接返回错误信息
   * <p>
   * 参数为：method,uri,inject => Resp[clientInterceptInfo]
   */
  private[rpc] var preExecuteInterceptor: (String, String, Any) => Resp[Map[String, String]] = { (method, uri, inject) => Resp.success(null) }
  /**
   * 后置执行方法
   * <p>
   * 参数为： (String, String) => Unit
   */
  private[rpc] var postExecuteInterceptor: (String, String) => Unit = { (method, uri) => Unit }
  /**
   * 通道类型
   * <p>
   *
   * [[com.ecfront.ez.framework.rpc.RPC.EChannel]]
   */
  private var channel = EChannel.HTTP
  /**
   * 处理类实例，不同的通道对应不同的类
   */
  private var processor: ClientProcessor = _

  /**
   * 获取当前服务所用的通道
   * @return 通道类型
   *
   *         [[com.ecfront.ez.framework.rpc.RPC.EChannel]]
   */
  def getChannel = {
    channel
  }

  /**
   * 设置服务连接通道，默认为HTTP
   * <p>
   * 支持 HTTP、Web Sockets 、 Event Bus
   * <p>
   * <ul>
   * <li>HTTP : 用于常规的HTTP服务，不支持Session、Cookies及HTTP Header定义</li>
   * <li>Web Sockets : 用于需要保持长连接的HTTP服务</li>
   * <li>Event Bus : 用于高性能的节点间通讯，支持集群负载均衡，支持publish subscribe 及 point to point </li>
   * </ul>
   *
   * @param channel  通道类型
   *
   *                 [[com.ecfront.ez.framework.rpc.RPC.EChannel]]
   */
  def setChannel(channel: EChannel) = {
    this.channel = channel
    this
  }

  /**
   * 设置服务连接端口（对Event Bus通道无效），默认为8080
   * @param port 服务连接的端口
   */
  def setPort(port: Int) = {
    this.port = port
    this
  }

  /**
   * 设置服务连接的IP（对Event Bus通道无效），默认为0.0.0.0
   * @param host 服务连接的IP
   */
  def setHost(host: String) = {
    this.host = host
    this
  }

  /**
   * 设置前置执行方法
   * <p>
   * 当返回 code=200 时表示执行成功，进入业务方法，反之表示执行出错，直接返回错误信息
   * @param preExecuteInterceptor ( method,uri,inject => Resp[clientInterceptInfo]  )
   */
  def setPreExecuteInterceptor(preExecuteInterceptor: => (String, String, Any) => Resp[Map[String, String]]) = {
    this.preExecuteInterceptor = preExecuteInterceptor
    this
  }

  /**
   * 设置后置执行方法
   * @param postExecuteInterceptor  ( method,uri => Unit )
   */
  def setPostExecuteInterceptor(postExecuteInterceptor: => (String, String) => Unit) = {
    this.postExecuteInterceptor = postExecuteInterceptor
    this
  }

  /**
   * 开启连接
   */
  def startup() = {
    processor = channel match {
      case EChannel.HTTP => new HttpClientProcessor
      case EChannel.EVENT_BUS => new EventBusClientProcessor
      case EChannel.WEB_SOCKETS => new WebSocketsClientProcessor
    }
    processor.init(port, host, this)
    this
  }

  /**
   * 关闭连接
   */
  def shutdown() {
    processor.destroy()
  }

  /**
   * 使用 publish subscribe 方式（仅对Event Bus有效）
   */
  def publish = {
    processor.isPointToPoint = false
    this
  }

  /**
   * 使用  point to point  方式（仅对Event Bus有效），默认使用此方式
   */
  def pointToPoint = {
    processor.isPointToPoint = true
    this
  }

  /**
   * GET请求（异步）
   * @param path 资源路径
   * @param responseClass 返回对象的类型
   * @param fun 业务方法
   * @param inject 注入信息
   * @return Result包装对象
   */
  def get[E](path: String, responseClass: Class[E] = null, fun: => Resp[E] => Unit = null, inject: Any = None) = {
    processor.process[E](Method.GET, path, null, responseClass, fun, inject)
    this
  }

  /**
   * GET请求（同步）
   * @param path 资源路径
   * @param responseClass 返回对象的类型
   * @param inject 注入信息
   * @return Result包装对象
   */
  def getSync[E](path: String, responseClass: Class[E] = null, inject: Any = None): Option[Resp[E]] = {
    Await.result(processor.process[E](Method.GET, path, null, responseClass, inject), Duration.Inf)
  }

  /**
   * DELETE请求（异步）
   * @param path 资源路径
   * @param responseClass 返回对象的类型
   * @param fun 业务方法
   * @param inject 注入信息
   * @return Result包装对象
   */
  def delete[E](path: String, responseClass: Class[E] = null, fun: => Resp[E] => Unit = null, inject: Any = None) = {
    processor.process[E](Method.DELETE, path, null, responseClass, fun, inject)
    this
  }

  /**
   * DELETE请求（同步）
   * @param path 资源路径
   * @param responseClass 返回对象的类型
   * @param inject 注入信息
   * @return Result包装对象
   */
  def deleteSync[E](path: String, responseClass: Class[E] = null, inject: Any = None): Option[Resp[E]] = {
    Await.result(processor.process[E](Method.DELETE, path, null, responseClass, inject), Duration.Inf)
  }

  /**
   * POST请求（异步）
   * @param path 资源路径
   * @param data 资源对象
   * @param responseClass 返回对象的类型
   * @param fun 业务方法
   * @param inject 注入信息
   * @return Result包装对象
   */
  def post[E](path: String, data: Any, responseClass: Class[E] = null, fun: => Resp[E] => Unit = null, inject: Any = None) = {
    processor.process[E](Method.POST, path, data, responseClass, fun, inject)
    this
  }

  /**
   * POST请求（同步）
   * @param path 资源路径
   * @param data 资源对象
   * @param responseClass 返回对象的类型
   * @param inject 注入信息
   * @return Result包装对象
   */
  def postSync[E](path: String, data: Any, responseClass: Class[E] = null, inject: Any = None): Option[Resp[E]] = {
    Await.result(processor.process[E](Method.POST, path, data, responseClass, inject), Duration.Inf)
  }

  /**
   * PUT请求（异步）
   * @param path 资源路径
   * @param data 资源对象
   * @param responseClass 返回对象的类型
   * @param fun 业务方法
   * @param inject 注入信息
   * @return Result包装对象
   */
  def put[E](path: String, data: Any, responseClass: Class[E] = null, fun: => Resp[E] => Unit = null, inject: Any = None) = {
    processor.process[E](Method.PUT, path, data, responseClass, fun, inject)
    this
  }

  /**
   * PUT请求（同步）
   * @param path 资源路径
   * @param data 资源对象
   * @param responseClass 返回对象的类型
   * @param inject 注入信息
   * @return Result包装对象
   */
  def putSync[E](path: String, data: Any, responseClass: Class[E] = null, inject: Any = None): Option[Resp[E]] = {
    Await.result(processor.process[E](Method.PUT, path, data, responseClass, inject), Duration.Inf)
  }

  /**
   * 返回原生对象的资源操作
   */
  def raw = new Raw

  class Raw {

    /**
     * GET请求（异步）
     * @param path 资源路径
     * @param responseClass 返回对象的类型
     * @param fun 业务方法
     * @param inject 注入信息
     * @return 原生对象
     */
    def get[E](path: String, responseClass: Class[E] = null, fun: => E => Unit = null, inject: Any = None) = {
      processor.processRaw[E](Method.GET, path, "", responseClass, fun, inject)
      this
    }

    /**
     * GET请求（同步）
     * @param path 资源路径
     * @param responseClass 返回对象的类型
     * @param inject 注入信息
     * @return 原生对象
     */
    def getSync[E](path: String, responseClass: Class[E] = null, inject: Any = None): Option[E] = {
      Await.result(processor.processRaw[E](Method.GET, path, "", responseClass, inject), Duration.Inf)
    }

    /**
     * DELETE请求（异步）
     * @param path 资源路径
     * @param responseClass 返回对象的类型
     * @param fun 业务方法
     * @param inject 注入信息
     * @return 原生对象
     */
    def delete[E](path: String, responseClass: Class[E] = null, fun: => E => Unit = null, inject: Any = None) = {
      processor.processRaw[E](Method.DELETE, path, "", responseClass, fun, inject)
      this
    }

    /**
     * DELETE请求（同步）
     * @param path 资源路径
     * @param responseClass 返回对象的类型
     * @param inject 注入信息
     * @return 原生对象
     */
    def deleteSync[E](path: String, responseClass: Class[E] = null, inject: Any = None): Option[E] = {
      Await.result(processor.processRaw[E](Method.DELETE, path, "", responseClass, inject), Duration.Inf)
    }

    /**
     * POST请求（异步）
     * @param path 资源路径
     * @param data 资源对象
     * @param responseClass 返回对象的类型
     * @param fun 业务方法
     * @param inject 注入信息
     * @return 原生对象
     */
    def post[E](path: String, data: Any, responseClass: Class[E] = null, fun: => E => Unit = null, inject: Any = None) = {
      processor.processRaw[E](Method.POST, path, data, responseClass, fun, inject)
      this
    }

    /**
     * POST请求（同步）
     * @param path 资源路径
     * @param data 资源对象
     * @param responseClass 返回对象的类型
     * @param inject 注入信息
     * @return 原生对象
     */
    def postSync[E](path: String, data: Any, responseClass: Class[E] = null, inject: Any = None): Option[E] = {
      Await.result(processor.processRaw[E](Method.POST, path, data, responseClass, inject), Duration.Inf)
    }

    /**
     * PUT请求（异步）
     * @param path 资源路径
     * @param data 资源对象
     * @param responseClass 返回对象的类型
     * @param fun 业务方法
     * @param inject 注入信息
     * @return 原生对象
     */
    def put[E](path: String, data: Any, responseClass: Class[E] = null, fun: => E => Unit = null, inject: Any = None) = {
      processor.processRaw[E](Method.PUT, path, data, responseClass, fun, inject)
      this
    }

    /**
     * PUT请求（同步）
     * @param path 资源路径
     * @param data 资源对象
     * @param responseClass 返回对象的类型
     * @param inject 注入信息
     * @return 原生对象
     */
    def putSync[E](path: String, data: Any, responseClass: Class[E] = null, inject: Any = None): Option[E] = {
      Await.result(processor.processRaw[E](Method.PUT, path, data, responseClass, inject), Duration.Inf)
    }

    /**
     * 关闭连接
     */
    def shutdown() {
      client.shutdown()
    }

  }

}
