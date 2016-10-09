package com.ecfront.ez.framework.service.gateway

import java.util.regex.Pattern

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc.{Channel, Method, RPCProcessor}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable

object LocalCacheContainer extends LazyLogging {

  private val routerContainer = Map[String, Map[String, collection.mutable.Set[String]]](
    Channel.HTTP.toString -> Map(
      Method.GET.toString -> collection.mutable.Set[String](),
      Method.POST.toString -> collection.mutable.Set[String](),
      Method.PUT.toString -> collection.mutable.Set[String](),
      Method.DELETE.toString -> collection.mutable.Set[String]()
    ),
    Channel.WS.toString -> Map(Method.WS.toString -> collection.mutable.Set[String]())
  )

  private val routerContainerR = Map[String, Map[String, collection.mutable.Set[RouterRContent]]](
    Channel.HTTP.toString -> Map(
      Method.GET.toString -> collection.mutable.Set[RouterRContent](),
      Method.POST.toString -> collection.mutable.Set[RouterRContent](),
      Method.PUT.toString -> collection.mutable.Set[RouterRContent](),
      Method.DELETE.toString -> collection.mutable.Set[RouterRContent]()
    ),
    Channel.WS.toString -> Map(Method.WS.toString -> collection.mutable.Set[RouterRContent]())
  )

  private val resources = Map[String, Map[String, collection.mutable.Set[(String, String)]]](
    Channel.HTTP.toString -> Map(
      Method.GET.toString -> collection.mutable.Set[(String, String)](),
      Method.POST.toString -> collection.mutable.Set[(String, String)](),
      Method.PUT.toString -> collection.mutable.Set[(String, String)](),
      Method.DELETE.toString -> collection.mutable.Set[(String, String)](),
      "*" -> collection.mutable.Set[(String, String)]()
    ),
    Channel.WS.toString -> Map(Method.WS.toString -> collection.mutable.Set[(String, String)]())
  )

  private val resourcesR = Map[String, Map[String, collection.mutable.Set[(String, String)]]](
    Channel.HTTP.toString -> Map(
      Method.GET.toString -> collection.mutable.Set[(String, String)](),
      Method.POST.toString -> collection.mutable.Set[(String, String)](),
      Method.PUT.toString -> collection.mutable.Set[(String, String)](),
      Method.DELETE.toString -> collection.mutable.Set[(String, String)](),
      "*" -> collection.mutable.Set[(String, String)]()
    ),
    Channel.WS.toString -> Map(Method.WS.toString -> collection.mutable.Set[(String, String)]())
  )

  private val organizations = collection.mutable.Set[String]()
  private val roles = collection.mutable.Map[String, Set[String]]()

  def addResource(channel: String, method: String, path: String): Resp[Void] = {
    if (path.endsWith("*")) {
      if (!resourcesR(channel)(method).exists(_._1 == path.substring(0, path.length - 1))) {
        resourcesR(channel)(method) += ((path.substring(0, path.length - 1), RPCProcessor.packageAddress(channel, method, path)))
      }
    } else {
      if (!resources(channel)(method).exists(_._1 == path)) {
        resources(channel)(method) += ((path, RPCProcessor.packageAddress(channel, method, path)))
      }
    }
    Resp.success(null)
  }

  def removeResource(channel: String, method: String, path: String): Resp[Void] = {
    if (path.endsWith("*")) {
      resourcesR(channel)(method) -= ((path.substring(0, path.length - 1), RPCProcessor.packageAddress(channel, method, path)))
    } else {
      resources(channel)(method) -= ((path, RPCProcessor.packageAddress(channel, method, path)))
    }
    Resp.success(null)
  }

  def getResourceCode(channel: String, method: String, path: String): String = {
    var res: Option[(String, String)] = None
    if (Channel.HTTP.toString == channel) {
      res = resourcesR(channel)("*").find(i => path.startsWith(i._1))
    }
    if (res.isDefined) {
      res.get._2
    } else {
      res = resourcesR(channel)(method).find(i => path.startsWith(i._1))
      if (res.isDefined) {
        res.get._2
      } else {
        if (Channel.HTTP.toString == channel) {
          res = resources(channel)("*").find(_._1 == path)
        }
        if (res.isDefined) {
          res.get._2
        } else {
          res = resources(channel)(method).find(_._1 == path)
          if (res.isDefined) {
            res.get._2
          } else {
            null
          }
        }
      }
    }
  }

  def addOrganization(code: String): Resp[Void] = {
    organizations += code
    Resp.success(null)
  }

  def removeOrganization(code: String): Resp[Void] = {
    organizations -= code
    Resp.success(null)
  }

  def existOrganization(code: String): Boolean = {
    organizations.contains(code)
  }

  def addRole(code: String, resourceCodes: Set[String]): Resp[Void] = {
    roles += code -> resourceCodes.map {
      resCode =>
        if (!resCode.endsWith("/")) {
          resCode + "/"
        } else {
          resCode
        }
    }
    Resp.success(null)
  }

  def removeRole(code: String): Resp[Void] = {
    roles -= code
    Resp.success(null)
  }

  def existResourceByRoles(roleCodes: Set[String], resCode: String): Boolean = {
    roleCodes.exists {
      roleCode =>
        roles.contains(roleCode) && roles(roleCode).contains(resCode)
    }
  }

  def addRouter(channel: String, method: String, path: String): Resp[Void] = {
    logger.info(s"Register [$channel] method [$method] path : $path")
    if (path.contains(":")) {
      // regular
      val r = getRegex(path)
      // 注册到正则路由表
      if (!routerContainerR(channel)(method).exists(_.originalPath == path)) {
        routerContainerR(channel)(method) += RouterRContent(path, r._1, r._2)
      }
    } else {
      // 注册到非正则路由表
      routerContainer(channel)(method) += path
    }
    Resp.success(null)
  }

  private[gateway] def getRouter(
                                  channel: String, method: String, path: String,
                                  parameters: Map[String, String], ip: String): (Resp[_], Map[String, String], String) = {
    val newParameters = collection.mutable.Map[String, String]()
    newParameters ++= parameters
    // 格式化path
    val formatPath = if (path.endsWith("/")) path else path + "/"
    var urlTemplate = formatPath
    if (routerContainer(channel).contains(method.toUpperCase)) {
      if (routerContainer(channel)(method.toUpperCase).contains(formatPath)) {
        urlTemplate = formatPath
      } else {
        // 使用正则路由
        routerContainerR(channel)(method).foreach {
          item =>
            val matcher = item.pattern.matcher(formatPath)
            if (matcher.matches()) {
              // 匹配到正则路由
              // 获取原始（注册时的）Path
              urlTemplate = item.originalPath
              // 从Path中抽取变量
              item.param.foreach(name => newParameters += (name -> matcher.group(name)))
            }
        }
      }
      if (urlTemplate != null) {
        // 匹配到路由
        (Resp.success(null), newParameters.toMap, urlTemplate)
      } else {
        // 没有匹配到路由
        logger.warn(s"[$channel] method [$method] path : $formatPath not implemented from $ip")
        (Resp.notImplemented(s"[$channel] method [$method] path : $formatPath from $ip"), newParameters.toMap, null)
      }
    } else {
      // 没有匹配到路由
      logger.warn(s"[$channel] method [$method] path : $formatPath not implemented from $ip")
      (Resp.notImplemented(s"[$channel] method [$method] path : $formatPath from $ip"), newParameters.toMap, null)
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

case class RouterRContent(originalPath: String, pattern: Pattern, param: Seq[String])
