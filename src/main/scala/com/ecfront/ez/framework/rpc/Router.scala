package com.ecfront.ez.framework.rpc

import java.util.regex.Pattern

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * 路由操作对象
 * @param requestClass 请求对象的类型
 * @param fun 业务方法
 */
case class Fun[E](requestClass: Class[E], private val fun: (Map[String, String], E, Any) => Any) {

  /**
   * 执行业务方法
   * @param parameters 请求参数
   * @param body 请求内容
   * @param inject 注入对象（前置执行的结果）
   * @return 执行结果
   */
  private[rpc] def execute(parameters: Map[String, String], body: Any, inject: Any): Any = {
    try {
      fun(parameters, body.asInstanceOf[E], inject)
    } catch {
      case e: Exception =>
        Resp.serverError(e.getMessage)
    }
  }

}

/**
 * 路由表
 * <p>
 * 不同的Server实例对应不同的路由实例
 */
private[rpc] class Router(server: Server) extends LazyLogging {

  /**
   * 业务方法容器，非正则
   * <p>
   * 结构为：method -> ( path -> ( 业务方法，服务实例 ) )
   */
  private val funContainer = collection.mutable.Map[String, collection.mutable.Map[String, Fun[_]]]()
  funContainer += ("POST" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("GET" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("DELETE" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("PUT" -> collection.mutable.Map[String, Fun[_]]())
  /**
   * 业务方法容器，正则
   * <p>
   *
   * [[com.ecfront.ez.framework.rpc.Router.RouterRContext]]
   */
  private val funContainerR = collection.mutable.Map[String, ArrayBuffer[RouterRContext]]()
  funContainerR += ("POST" -> ArrayBuffer[RouterRContext]())
  funContainerR += ("GET" -> ArrayBuffer[RouterRContext]())
  funContainerR += ("DELETE" -> ArrayBuffer[RouterRContext]())
  funContainerR += ("PUT" -> ArrayBuffer[RouterRContext]())

  /**
   * 获取对应的路由信息，先按非正则匹配，匹配再从正则容器中查找
   * @param method 资源操作方式
   * @param path 资源路径
   * @param parameters 要附加的参数（对于正则，从Path中抽取变量）
   * @param interceptInfo client拦截器添加的信息
   * @return （匹配结果，业务方法，新的参数，后置执行方法）
   */
  private[rpc] def getFunction(method: String, path: String, parameters: Map[String, String], interceptInfo: Map[String, String]): (Resp[_], Fun[_], Map[String, String], Any => Any) = {
    val newParameters = collection.mutable.Map[String, String]()
    newParameters ++= parameters
    var urlTemplate = path
    var fun: Fun[_] = funContainer(method.toUpperCase).get(path).orNull
    if (fun == null) {
      //使用正则路由
      funContainerR(method).foreach {
        item =>
          val matcher = item.pattern.matcher(path)
          if (matcher.matches()) {
            //匹配到正则路由
            //获取原始（注册时的）Path
            urlTemplate = item.originalPath
            fun = item.fun
            //从Path中抽取变量
            item.param.foreach(name => newParameters += (name -> matcher.group(name)))
          }
      }
    }
    if (fun != null || server.any != null) {
      //匹配到路由
      //先执行前置处理方法
      val result = server.preExecuteInterceptor(method, urlTemplate, newParameters.toMap, interceptInfo)
      if (result) {
        //前置处理方法执行成功
        (result, fun, newParameters.toMap, server.postExecuteInterceptor)
      } else {
        //前置处理方法执行失败
        (result, null, newParameters.toMap, null)
      }
    } else {
      //没有匹配到路由且没有any实现
      (Resp.notImplemented("[ %s ] %s".format(method, path)), null, newParameters.toMap, null)
    }
  }

  /**
   * 注册路由规则
   * @param method 资源操作方式
   * @param path 资源路径
   * @param requestClass 请求对象的类型
   * @param fun 业务方法
   */
  private[rpc] def add[E](method: String, path: String, requestClass: Class[E], fun: => (Map[String, String], E, Any) => Any) {
    //格式化URL
    val nPath = server.formatUri(path)
    logger.info(s"Register [${server.getChannel}] method [$method] path : $nPath.")
    if (nPath.contains(":")) {
      //regular
      val r = Router.getRegex(nPath)
      //注册到正则路由表
      funContainerR(method) += RouterRContext(nPath, r._1, r._2, Fun[E](requestClass, fun))
      //调用处理勾子方法
      server.processor.process(method, r._1.pattern(), isRegex = true)
    } else {
      //注册到非正则路由表
      funContainer(method) += (nPath -> Fun[E](requestClass, fun))
      //调用处理勾子方法
      server.processor.process(method, nPath, isRegex = false)
    }
  }

  /**
   * 正则路由上下文
   * @param originalPath 原始的Path（注册时用的Path）
   * @param pattern 正则对象
   * @param param 从原始Path中抽取的变量，如 /index/:id/ 则获取到 seq("id")
   * @param fun 业务方法
   */
  case class RouterRContext(originalPath: String, pattern: Pattern, param: Seq[String], fun: Fun[_])

}

object Router {

  /**
   * 将非规范正则转成规范正则
   * <p>
   * 如 输入 /index/:id/  输出 （^/index/(?<id>[^/]+)/$ 的正则对象，Seq("id") ）
   * @param path 非规范正则，用 :x 表示一个变量
   * @return （规范正则，变更列表）
   */
  private def getRegex(path: String): (Pattern, Seq[String]) = {
    var pathR = path
    var named = mutable.Buffer[String]()
    """:\w+""".r.findAllMatchIn(path).foreach {
      m =>
        val name = m.group(0).substring(1)
        pathR = pathR.replaceAll(m.group(0), """(?<""" + name + """>[^/]+)""")
        named += name
    }
    (Pattern.compile("^" + pathR + "$"), named)
  }

}
