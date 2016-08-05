package com.ecfront.ez.framework.service.rpc.foundation

import java.util.regex.Pattern

import com.ecfront.common.{BeanHelper, Resp}
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.service.storage.foundation.{Label, Require}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * 路由操作对象
  *
  * @param requestClass 请求对象的类型
  * @param fun          业务方法
  * @tparam E 请求对象的类型
  */
case class Fun[E](requestClass: Class[E], private val fun: (Map[String, String], E, EZRPCContext) => Resp[Any]) {

  private val allAnnotations =
    if(requestClass!=null){
    BeanHelper.findFieldAnnotations(requestClass).toList
  }else{
    List()
  }
  private val fieldLabel = allAnnotations.filter(_.annotation.isInstanceOf[Label]).map {
    field =>
      field.fieldName -> field.annotation.asInstanceOf[Label].label
  }.toMap
  private val requireFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[Require]).map {
    field =>
      field.fieldName
  }

  /**
    * 执行业务方法
    *
    * @param parameters 请求参数
    * @param body       请求体
    * @param context    RPC上下文
    * @return 执行结果
    */
  private[rpc] def execute(parameters: Map[String, String], body: Any, context: EZRPCContext): Resp[Any] = {
    body match {
      case bean: AnyRef =>
        val errorFields = requireFieldNames.filter(BeanHelper.getValue(bean, _).get == null).map {
          requireField =>
            if (fieldLabel.contains(requireField)) {
              fieldLabel(requireField).x
            } else {
              requireField.x
            }
        }
        if (errorFields.nonEmpty) {
          Resp.badRequest(errorFields.mkString("[", ",", "]") + " not null")
        } else {
          fun(parameters, body.asInstanceOf[E], context)
        }
      case _ =>
        fun(parameters, body.asInstanceOf[E], context)
    }
  }

}

/**
  * 路由表
  */
private[rpc] class Router extends LazyLogging {

  /**
    * 业务方法容器，非正则
    * 结构为：method -> ( path -> 业务方法)
    */
  private val funContainer = collection.mutable.Map[String, collection.mutable.Map[String, Fun[_]]]()
  funContainer += ("POST" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("GET" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("DELETE" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("PUT" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("REQUEST" -> collection.mutable.Map[String, Fun[_]]())

  /**
    * 业务方法容器，正则
    * 结构为：method -> 正则对象
    */
  private val funContainerR = collection.mutable.Map[String, ArrayBuffer[RouterRContent]]()
  funContainerR += ("POST" -> ArrayBuffer[RouterRContent]())
  funContainerR += ("GET" -> ArrayBuffer[RouterRContent]())
  funContainerR += ("DELETE" -> ArrayBuffer[RouterRContent]())
  funContainerR += ("PUT" -> ArrayBuffer[RouterRContent]())
  funContainerR += ("REQUEST" -> ArrayBuffer[RouterRContent]())

}

/**
  * 正则路由对象
  *
  * @param originalPath 原始的Path（注册时用的Path）
  * @param pattern      正则对象
  * @param param        从原始Path中抽取的变量，如 /index/:id/ 则获取到 seq("id")
  * @param fun          业务方法
  */
case class RouterRContent(originalPath: String, pattern: Pattern, param: Seq[String], fun: Fun[_])

object Router extends LazyLogging {

  // 路由容器 通道-> 路由表
  private val ROUTERS = collection.mutable.Map[String, Router]()

  /**
    * 获取对应的路由信息，先按非正则匹配，匹配再从正则容器中查找
    *
    * @param channel    RPC通道
    * @param method     请求方法
    * @param path       请求路径
    * @param parameters 请求参数
    * @param ip         请求的IP
    * @return 结果 （是否找到，业务方法，解析后的参数，对应的模板URI）
    */
  private[rpc] def getFunction(
                                channel: String, method: String, path: String,
                                parameters: Map[String, String], ip: String): (Resp[_], Fun[_], Map[String, String], String) = {
    val newParameters = collection.mutable.Map[String, String]()
    newParameters ++= parameters
    // 格式化path
    val formatPath = if (path.endsWith("/")) path else path + "/"
    var urlTemplate = formatPath
    if (ROUTERS(channel).funContainer.contains(method.toUpperCase)) {
      var fun: Fun[_] = ROUTERS(channel).funContainer(method.toUpperCase).get(formatPath).orNull
      if (fun == null) {
        // 使用正则路由
        ROUTERS(channel).funContainerR(method).foreach {
          item =>
            val matcher = item.pattern.matcher(formatPath)
            if (matcher.matches()) {
              // 匹配到正则路由
              // 获取原始（注册时的）Path
              urlTemplate = item.originalPath
              fun = item.fun
              // 从Path中抽取变量
              item.param.foreach(name => newParameters += (name -> matcher.group(name)))
            }
        }
      }
      if (fun != null) {
        // 匹配到路由
        (Resp.success(null), fun, newParameters.toMap, urlTemplate)
      } else {
        // 没有匹配到路由
        logger.warn(s"$method:$path not implemented from $ip")
        (Resp.notImplemented(s"$method:$path from $ip"), null, newParameters.toMap, null)
      }
    } else {
      // 没有匹配到路由
      logger.warn(s"$method:$path not implemented from $ip")
      (Resp.notImplemented(s"$method:$path from $ip"), null, newParameters.toMap, null)
    }
  }

  /**
    * 注册路由规则
    *
    * @param channel      通道
    * @param method       请求方法
    * @param path         请求路径
    * @param requestClass 请求对象的类型
    * @param fun          业务方法
    */
  private[rpc] def add[E](
                           channel: String, method: String, path: String, requestClass: Class[E],
                           fun: => (Map[String, String], E, EZRPCContext) => Resp[Any]): Unit = {
    if (!ROUTERS.contains(channel)) {
      ROUTERS += channel -> new Router
    }
    // 格式化path
    val formatPath = if (path.endsWith("/")) path else path + "/"
    logger.info(s"Register [${channel.toString}] method [$method] path : $formatPath.")
    if (formatPath.contains(":")) {
      // regular
      val r = Router.getRegex(formatPath)
      // 注册到正则路由表
      ROUTERS(channel).funContainerR(method) += RouterRContent(formatPath, r._1, r._2, Fun[E](requestClass, fun))
    } else {
      // 注册到非正则路由表
      ROUTERS(channel).funContainer(method) += (formatPath -> Fun[E](requestClass, fun))
    }
  }

  private val matchRegex =""":\w+""".r

  /**
    * 将非规范正则转成规范正则
    *
    * 如 输入 /index/:id/  输出 （^/index/(?<id>[^/]+)/$ 的正则对象，Seq("id") ）
    *
    * @param path 非规范正则，用 :x 表示一个变量
    * @return （规范正则，变更列表）
    */
  private def getRegex(path: String): (Pattern, Seq[String]) = {
    var pathR = path
    var named = mutable.Buffer[String]()
    matchRegex.findAllMatchIn(path).foreach {
      m =>
        val name = m.group(0).substring(1)
        pathR = pathR.replaceAll(m.group(0), """(?<""" + name + """>[^/]+)""")
        named += name
    }
    (Pattern.compile("^" + pathR + "$"), named)
  }

}