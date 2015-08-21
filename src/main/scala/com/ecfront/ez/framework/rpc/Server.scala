package com.ecfront.ez.framework.rpc

import com.ecfront.common.{ClassScanHelper, Resp}
import com.ecfront.ez.framework.rpc.RPC.EChannel
import com.ecfront.ez.framework.rpc.RPC.EChannel.EChannel
import com.ecfront.ez.framework.rpc.autobuilding.AutoBuildingProcessor
import com.ecfront.ez.framework.rpc.process.ServerProcessor
import com.ecfront.ez.framework.rpc.process.channel.eb.EventBusServerProcessor
import com.ecfront.ez.framework.rpc.process.channel.http.HttpServerProcessor
import com.ecfront.ez.framework.rpc.process.channel.websockets.WebSocketsServerProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.runtime._

/**
 * RPC服务
 * <p>
 * 支持标准的基于Json的Restful风格，返回结果为统一的Result或自定义对象
 */
case class Server() extends LazyLogging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  /**
   * 上传文件的根路径，只对HTTP通道有效，默认为/tmp/
   */
  private var rootUploadPath = "/tmp/"

  /**
   * 服务注册的端口（对Event Bus通道无效）
   */
  private var port = 8080

  /**
   * 服务注册的IP（对Event Bus通道无效）
   */
  private var host = "0.0.0.0"
  /**
   * 通道类型
   * <p>
   * 支持 HTTP、Web Sockets 、 Event Bus
   * <p>
   * <ul>
   * <li>HTTP : 用于常规的HTTP服务，不支持Session、Cookies及HTTP Header定义</li>
   * <li>Web Sockets : 用于需要保持长连接的HTTP服务</li>
   * <li>Event Bus : 用于高性能的节点间通讯，支持集群负载均衡，支持publish subscribe 及 point to point </li>
   * </ul>
   *
   * [[com.ecfront.ez.framework.rpc.RPC.EChannel]]
   */
  private var channel = EChannel.HTTP

  /**
   * 处理类实例，不同的通道对应不同的类
   */
  private[rpc] var processor: ServerProcessor = _

  /**
   * 路径表实例
   */
  private val router = new Router(this)

  /**
   * 后置接收方法，可用于权限过滤
   * <p>
   * 当返回 code=200 时表示执行成功，进入业务方法，反之表示执行出错，直接返回错误信息
   * <p>
   * 参数为：method,uri,parameters,clientInterceptInfo => Resp[inject]
   */
  private[rpc] var preExecuteInterceptor: (String, String, Map[String, String], Map[String, String]) => Resp[_] = { (method, uri, parameter, clientInterceptInfo) => Resp.success(null) }

  /**
   * 后置执行方法
   * <p>
   * 参数为： 业务方法返回值 =>  any obj
   */
  private[rpc] var postExecuteInterceptor: Any => Any = { obj => obj }

  /**
   * 无法匹配业务的处理方法
   * <p>
   * 参数为：method,uri,parameters,body,inject => Resp[Any]
   */
  private[rpc] var any: (String, String, Map[String, String], Any, Any) => Resp[_] = null


  /**
   * 全局URI格式化方法
   * <p>
   * 参数为：格式前的原始URI => 格式化的URI
   */
  private[rpc] var formatUri: String => String = { String => String }

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
   * 设置服务通道，默认为HTTP
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
   * 设置服务端口（对Event Bus通道无效），默认为8080
   * @param port 服务注册的端口
   */
  def setPort(port: Int) = {
    this.port = port
    this
  }

  /**
   * 设置服务注册的IP（对Event Bus通道无效），默认为0.0.0.0
   * @param host 服务注册的IP
   */
  def setHost(host: String) = {
    this.host = host
    this
  }

  /**
   * 设置上传文件的根路径，只对HTTP通道有效，默认为/tmp/
   * @param rootUploadPath 上传文件的根路径
   */
  def setRootUploadPath(rootUploadPath: String) = {
    this.rootUploadPath = rootUploadPath
    this
  }

  /**
   * 格式化URI
   * @param formatUri  ( source uri => dest uri )
   */
  def setFormatUri(formatUri: => String => String) = {
    this.formatUri = formatUri
    this
  }

  /**
   * 设置前置执行方法
   * <p>
   * 当返回 code=200 时表示执行成功，进入业务方法，反之表示执行出错，直接返回错误信息
   * @param preExecuteInterceptor ( method,uri,parameters,clientInterceptInfo => Resp[inject] )
   */
  def setPreExecuteInterceptor(preExecuteInterceptor: => (String, String, Map[String, String], Map[String, String]) => Resp[_]) = {
    this.preExecuteInterceptor = preExecuteInterceptor
    this
  }

  /**
   * 设置后置执行方法
   * @param postExecuteInterceptor  ( 业务方法返回值 =>  any obj )
   */
  def setPostExecuteInterceptor(postExecuteInterceptor: => Any => Any) = {
    this.postExecuteInterceptor = postExecuteInterceptor
    this
  }

  /**
   * 设置无法匹配业务的处理方法
   * @param any  ( method,uri,parameters,body,inject => Resp[Any] )
   */
  def setAny(any: => (String, String, Map[String, String], Any, Any) => Resp[_]) = {
    this.any = any
    this
  }

  /**
   * 启动服务
   */
  def startup(): Server = {
    logger.info("RPC Service starting...")
    processor = channel match {
      case EChannel.HTTP => new HttpServerProcessor
      case EChannel.EVENT_BUS => new EventBusServerProcessor
      case EChannel.WEB_SOCKETS => new WebSocketsServerProcessor
    }
    processor.init(port, host, router, rootUploadPath, this)
    logger.info("RPC Service is running at %s:%s:%s".format(channel.toString, host, port))
    this
  }

  /**
   * 关闭服务
   */
  def shutdown() {
    processor.destroy()
  }

  /**
   * 使用基于注解的自动构建，此方法必须在服务启动“startup”后才能调用
   * @param instance 目标对象
   */
  def autoBuilding(instance: AnyRef) = {
    AutoBuildingProcessor.process(this, instance)
    this
  }

  /**
   * 使用基于注解的自动构建，此方法必须在服务启动“startup”后才能调用
   * @param basePackage  服务类所在的根包名
   */
  def autoBuilding(basePackage: String) = {
    ClassScanHelper.scan[RPC](basePackage).foreach {
      clazz =>
        if (clazz.getSimpleName.endsWith("$")) {
          AutoBuildingProcessor.process(this, runtimeMirror.reflectModule(runtimeMirror.staticModule(clazz.getName)).instance.asInstanceOf[AnyRef])
        } else {
          AutoBuildingProcessor.process(this, clazz.newInstance().asInstanceOf[AnyRef])
        }
    }
    this
  }

  /**
   * 注册POST方法
   * @param path 资源路径
   * @param requestClass 请求对象的类型
   * @param function 业务方法
   */
  def post[E](path: String, requestClass: Class[E], function: (Map[String, String], E, Any) => Any) = {
    router.add(Method.POST, path, requestClass, function)
    this
  }

  /**
   * 注册PUT方法
   * @param path 资源路径
   * @param requestClass 请求对象的类型
   * @param function 业务方法
   */
  def put[E](path: String, requestClass: Class[E], function: => (Map[String, String], E, Any) => Any) = {
    router.add(Method.PUT, path, requestClass, function)
    this
  }


  /**
   * 注册DELETE方法
   * @param path 资源路径
   * @param function 业务方法
   */
  def delete(path: String, function: => (Map[String, String], Void, Any) => Any) = {
    router.add(Method.DELETE, path, classOf[Void], function)
    this
  }


  /**
   * 注册GET方法
   * @param path 资源路径
   * @param function 业务方法
   */
  def get(path: String, function: => (Map[String, String], Void, Any) => Any) = {
    router.add(Method.GET, path, classOf[Void], function)
    this
  }

  /**
   * 反射调用，反射时避免类型type处理
   */
  object reflect {
    /**
     * 注册POST方法
     * @param path 资源路径
     * @param requestClass 请求对象的类型
     * @param function 业务方法
     */
    def post(path: String, requestClass: Class[_], function: (Map[String, String], Any, Any) => Any) = {
      router.add(Method.POST, path, requestClass, function)
      this
    }

    /**
     * 注册PUT方法
     * @param path 资源路径
     * @param requestClass 请求对象的类型
     * @param function 业务方法
     */
    def put(path: String, requestClass: Class[_], function: => (Map[String, String], Any, Any) => Any) = {
      router.add(Method.PUT, path, requestClass, function)
      this
    }

    /**
     * 注册DELETE方法
     * @param path 资源路径
     * @param function 业务方法
     */
    def delete(path: String, function: => (Map[String, String], Void, Any) => Any) = {
      router.add(Method.DELETE, path, classOf[Void], function)
      this
    }

    /**
     * 注册GET方法
     * @param path 资源路径
     * @param function 业务方法
     */
    def get(path: String, function: => (Map[String, String], Void, Any) => Any) = {
      router.add(Method.GET, path, classOf[Void], function)
      this
    }
  }

}
