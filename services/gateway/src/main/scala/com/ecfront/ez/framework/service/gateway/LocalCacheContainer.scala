package com.ecfront.ez.framework.service.gateway

import java.util.regex.Pattern

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.Method

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object LocalCacheContainer extends Logging {

  private val routerContainer = Map[String, collection.mutable.Set[String]](
    Method.GET.toString -> collection.mutable.Set[String](),
    Method.POST.toString -> collection.mutable.Set[String](),
    Method.PUT.toString -> collection.mutable.Set[String](),
    Method.DELETE.toString -> collection.mutable.Set[String](),
    Method.WS.toString -> collection.mutable.Set[String]()
  )

  private val routerContainerR = Map[String, collection.mutable.Set[RouterRContent]](
    Method.GET.toString -> collection.mutable.Set[RouterRContent](),
    Method.POST.toString -> collection.mutable.Set[RouterRContent](),
    Method.PUT.toString -> collection.mutable.Set[RouterRContent](),
    Method.DELETE.toString -> collection.mutable.Set[RouterRContent](),
    Method.WS.toString -> collection.mutable.Set[RouterRContent]()
  )

  private val resources = Map[String, collection.mutable.Map[String, ArrayBuffer[String]]](
    Method.GET.toString -> collection.mutable.Map[String, ArrayBuffer[String]](),
    Method.POST.toString -> collection.mutable.Map[String, ArrayBuffer[String]](),
    Method.PUT.toString -> collection.mutable.Map[String, ArrayBuffer[String]](),
    Method.DELETE.toString -> collection.mutable.Map[String, ArrayBuffer[String]](),
    Method.WS.toString -> collection.mutable.Map[String, ArrayBuffer[String]]()
  )

  val resourceROrderByPathDepth = Ordering.by[(String, ArrayBuffer[String]), Int](_._1.split("/").length * -1)
  private val resourcesR = Map[String, collection.mutable.SortedSet[(String, ArrayBuffer[String])]](
    Method.GET.toString -> collection.mutable.SortedSet[(String, ArrayBuffer[String])]()(resourceROrderByPathDepth),
    Method.POST.toString -> collection.mutable.SortedSet[(String, ArrayBuffer[String])]()(resourceROrderByPathDepth),
    Method.PUT.toString -> collection.mutable.SortedSet[(String, ArrayBuffer[String])]()(resourceROrderByPathDepth),
    Method.DELETE.toString -> collection.mutable.SortedSet[(String, ArrayBuffer[String])]()(resourceROrderByPathDepth),
    Method.WS.toString -> collection.mutable.SortedSet[(String, ArrayBuffer[String])]()(resourceROrderByPathDepth)
  )

  private val organizations = collection.mutable.Set[String]()

  private def formatResourcePath(path: String): String = {
    if (!path.endsWith("*") && !path.endsWith("/")) {
      path + "/"
    } else {
      path
    }
  }

  def addResource(method: String, _path: String): Resp[Void] = {
    val path = formatResourcePath(_path)
    if (path.endsWith("*")) {
      def add(m: String): Unit = {
        if (!resourcesR(m).exists(_._1 == path.substring(0, path.length - 1))) {
          resourcesR(m) += ((path.substring(0, path.length - 1), ArrayBuffer()))
        }
      }
      if (method == "*") {
        add(Method.GET.toString)
        add(Method.POST.toString)
        add(Method.PUT.toString)
        add(Method.DELETE.toString)
        add(Method.WS.toString)
      } else {
        add(method)
      }
    } else {
      def add(m: String): Unit = {
        if (!resources(m).contains(path)) {
          resources(m) += path -> ArrayBuffer()
        }
      }
      if (method == "*") {
        add(Method.GET.toString)
        add(Method.POST.toString)
        add(Method.PUT.toString)
        add(Method.DELETE.toString)
        add(Method.WS.toString)
      } else {
        add(method)
      }
    }
    Resp.success(null)
  }

  def removeResource(method: String, _path: String): Resp[Void] = {
    val path = formatResourcePath(_path)
    if (path.endsWith("*")) {
      def remove(m: String): Unit = {
        val removeRes = resourcesR(m).find(_._1 == path.substring(0, path.length - 1))
        if (removeRes.nonEmpty) {
          resourcesR(m) -= removeRes.get
        }
      }
      if (method == "*") {
        remove(Method.GET.toString)
        remove(Method.POST.toString)
        remove(Method.PUT.toString)
        remove(Method.DELETE.toString)
        remove(Method.WS.toString)
      } else {
        remove(method)
      }
    } else {
      if (resources(method).contains(path)) {
        resources(method) -= path
      }
    }
    Resp.success(null)
  }

  def existResourceByRoles(method: String, _path: String, roleCodes: Set[String]): Boolean = {
    val path = formatResourcePath(_path)
    roleCodes.exists(existResourceByRoles(method, path, _))
  }

  /**
    * 逻辑：
    * 1）先找正常的资源表，找到资源且匹配到角色返回true，找不到资源或对应的资源没有匹配到角色则进入下一步
    * 2）再找模糊资源表，
    * 找到资源且匹配到角色返回true
    * 找到资源但没有匹配到角色返回false
    * 第一步找不资源且这一步也找不到资源且返回true
    * 第一步找到资源但没有匹配到角色，这一步找不到资源返回false
    *
    * @return
    * true: 1）找不资源，表示此资源不需要认证，2）找到资源且匹配到角色
    * false: 找到资源但没有匹配到角色
    */
  private def existResourceByRoles(method: String, path: String, roleCode: String): Boolean = {
    if (resources(method).contains(path)) {
      // found resource
      if (!resources(method).get(path).exists(_.contains(roleCode))) {
        // not found role
        val resR = resourcesR(method).filter(i => path.startsWith(i._1))
        if (resR.nonEmpty) {
          // match resource by regex
          resR.exists(_._2.contains(roleCode))
        } else {
          // found resource but not matched role and resourceR not found
          false
        }
      } else {
        // matched role
        true
      }
    } else {
      // not found resource
      val resR = resourcesR(method).filter(i => path.startsWith(i._1))
      if (resR.nonEmpty) {
        // match resource by regex
        resR.exists(_._2.contains(roleCode))
      } else {
        // not found resource & resourceR
        true
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
    resourceCodes.foreach {
      _resCode =>
        val resCode = formatResourcePath(_resCode)
        val Array(method, path) = resCode.split(EZ.eb.ADDRESS_SPLIT_FLAG)
        if (path.endsWith("*")) {
          def add(m: String): Unit = {
            val resR = resourcesR(m.toString).find(_._1 == path.substring(0, path.length - 1))
            if (resR.isDefined) {
              resR.get._2 += code
            } else {
              resourcesR(m.toString) += ((path.substring(0, path.length - 1), ArrayBuffer(code)))
            }
          }
          if (method == "*") {
            add(Method.GET.toString)
            add(Method.POST.toString)
            add(Method.PUT.toString)
            add(Method.DELETE.toString)
            add(Method.WS.toString)
          } else {
            add(method)
          }
        } else {
          def add(m: String): Unit = {
            val resR = resources(Method.GET.toString).get(path)
            if (resR.isDefined) {
              resR.get += code
            } else {
              resources(m.toString) += path -> ArrayBuffer(code)
            }
          }
          if (method == "*") {
            add(Method.GET.toString)
            add(Method.POST.toString)
            add(Method.PUT.toString)
            add(Method.DELETE.toString)
            add(Method.WS.toString)
          } else {
            add(method)
          }
        }
    }
    Resp.success(null)
  }

  def removeRole(code: String): Resp[Void] = {
    resourcesR.values.flatten.foreach(_._2 -= code)
    resources.values.flatten.foreach(_._2 -= code)
    Resp.success(null)
  }

  def flushAuth(): Resp[Void] = {
    resourcesR.foreach(_._2.empty)
    resources.foreach(_._2.empty)
    organizations.empty
    Resp.success(null)
  }

  def addRouter(method: String, path: String): Resp[Void] = {
    val formatPath = if (path.endsWith("/")) path else path + "/"
    logger.info(s"Register method [$method] path : $formatPath")
    if (formatPath.contains(":")) {
      // regular
      val r = getRegex(formatPath)
      // 注册到正则路由表
      if (!routerContainerR(method).exists(_.originalPath == formatPath)) {
        routerContainerR(method) += RouterRContent(formatPath, r._1, r._2)
      }
    } else {
      // 注册到非正则路由表
      routerContainer(method) += formatPath
    }
    Resp.success(null)
  }

  private[gateway] def getRouter(method: String, path: String,
                                 parameters: Map[String, String], ip: String): (Resp[_], Map[String, String], String) = {
    val newParameters = collection.mutable.Map[String, String]()
    newParameters ++= parameters
    // 格式化path
    val formatPath = if (path.endsWith("/")) path else path + "/"
    var urlTemplate:String=null
    if (routerContainer.contains(method.toUpperCase)) {
      if (routerContainer(method.toUpperCase).contains(formatPath)) {
        urlTemplate = formatPath
      } else {
        // 使用正则路由
        routerContainerR(method).foreach {
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
        logger.warn(s"method [$method] path : $formatPath not implemented from $ip")
        (Resp.notImplemented(s"method [$method] path : $formatPath from $ip"), newParameters.toMap, null)
      }
    } else {
      // 没有匹配到路由
      logger.warn(s"method [$method] path : $formatPath not implemented from $ip")
      (Resp.notImplemented(s"method [$method] path : $formatPath from $ip"), newParameters.toMap, null)
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
