package com.ecfront.ez.framework.rpc.process.channel.eb

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

import com.ecfront.ez.framework.rpc.cluster.ClusterManager
import com.ecfront.ez.framework.rpc.process.Processor
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.eventbus.{EventBus, Message}
import io.vertx.core.json.JsonObject
import io.vertx.core.spi.cluster.{AsyncMultiMap, ChoosableIterable}
import io.vertx.core.{AsyncResult, Handler}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * Event Bus处理辅助类
 * <p>
 * Event Bus处理的难点在于 `无法直接处理带正则的请求`
 * <p>
 * 如注册了 <code>/index/:id/</code> 这样的路由，Client 使用  <code>/index/1/</code> 请求就无法找到服务地址，
 * <p>
 * 因为不像HTTP有明确的请求目标节点，可以让服务器接收所有请求后内部通过路由表判断，
 * <p>
 * Event Bus用于集群，有多个节点，不同节点可能代表不同的业务，有不同的路由表，处理不同请求，所以像 <code>/index/1/</code> 这样的请求就不可能随便发到某个节点再让节点内部处理。
 * <p>
 * 解决的方法是 `使用全局（集群）路由映射表 `
 * <ul>
 * <li>1.  初始服务节点时获取集群中的路由映射表 <code>clusterAddressPathMap </code> </li>
 * <li>2. 从集群中获取正则与非正则地址到本地路由表 <code>localAddressPathMap </code> <code>localRAddressPathList </code> </li>
 * <li>3. 注册正则化处理后的服务地址更新事件（地址：__ADD_ADDRESS_PATH__），用于接收新的服务地址并更新到本地路由表 </li>
 * <li>4. 请求时从本地路由表中匹配，返回正则化的地址，此地址会与注册的地址对应，故可以找到实际的服务节点 </li>
 * <li>4. 请求时把原始的地址加到 __path__ 变量中，服务端可以获取到原始Path以做进一步处理 </li>
 * </ul>
 */
trait EventBusOptions extends Processor {


  protected val FLAG_IS_REPLY: String = "__isReply__"

  /**
   * 初始化Event Bus
   * @param host 保留参数，暂时无效
   * @param port 保留参数，暂时无效
   */
  protected def initEventbus(host: String, port: Int): Unit = {
    if (!EventBusOptions.existInstance.getAndSet(true)) {
      val latch = new CountDownLatch(2)
      //Step 1 : 初始化集群的Event Bus
      ClusterManager.initCluster(host)
      EventBusOptions.eb = ClusterManager.vertx.eventBus()
      //Step 2 : 向集群注册服务路由映射Map
      ClusterManager.clusterManager.getAsyncMultiMap[String, String]("__REGISTER_MAP_ADDRESS_PATH__", new Handler[AsyncResult[AsyncMultiMap[String, String]]] {
              override def handle(event: AsyncResult[AsyncMultiMap[String, String]]): Unit = {
                if (event.succeeded()) {
                  //Step 3 : 获取集群路由映射表
                  EventBusOptions.clusterAddressPathMap = event.result()
                  logger.info("Init REGISTER_MAP_ADDRESS_PATH success.")
                  //Step 4：从集群中获取正则与非正则地址到本地路由表
                  getAddressPathInCluster(false, {
                    result =>
                      EventBusOptions.localAddressPathMap ++= result.map(_ -> null)
                      latch.countDown()
                  })
                  getAddressPathInCluster(true, {
                    result =>
                      EventBusOptions.localRAddressPathList ++= result.map(_.r.pattern)
                      latch.countDown()
                  })
                } else {
                  logger.error("Init REGISTER_MAP_ADDRESS_PATH failed!", event.cause())
                }
              }
            })
      latch.await()
      //Step 5 : 注册服务地址更新事件，用于接收新的服务地址并更新到本地路由表
      EventBusOptions.eb.consumer("__ADD_ADDRESS_PATH__", new Handler[Message[JsonObject]] {
        override def handle(event: Message[JsonObject]): Unit = {
          val address = event.body().getString("address")
          if (!event.body().getBoolean("isRegex")) {
            EventBusOptions.localAddressPathMap += address -> null
          } else {
            EventBusOptions.localRAddressPathList += address.r.pattern
          }
          logger.info(s"Register $address to cluster success.")
        }
      })
      EventBusOptions.startFinish = true
    } else {
      while (!EventBusOptions.startFinish) {
        Thread.sleep(100)
        logger.info("EventBus has exist,wait start finish...")
      }
    }
  }

  /**
   * 从集群中获取服务路由表
   * @param useRegex 是否是正则路由
   * @param callback 获取成功后的回调方法
   */
  private def getAddressPathInCluster(useRegex: Boolean, callback: => List[String] => Unit): Unit = {
    EventBusOptions.clusterAddressPathMap.get(if (useRegex) "__r_address_path__" else "__address_path__", new Handler[AsyncResult[ChoosableIterable[String]]] {
      override def handle(event: AsyncResult[ChoosableIterable[String]]): Unit = {
        if (event.succeeded()) {
          //正则或非正则的服务路由表
          callback(event.result().iterator().toList)
        } else {
          logger.error(s"Get REGISTER_MAP_ADDRESS_PATH failed!", event.cause())
        }
      }
    })
  }

  protected def destoryEventbus(): Unit = {
    val latch = new CountDownLatch(1)
    EventBusOptions.eb.close(new Handler[AsyncResult[Void]] {
      override def handle(event: AsyncResult[Void]): Unit = {
        if (event.succeeded()) {
          latch.countDown()
        } else {
          logger.error("Shutdown failed.", event.cause())
        }
      }
    })
    //TODO remove clusterAddressPathMap
    EventBusOptions.existInstance.set(false)
    EventBusOptions.startFinish = false
    ClusterManager.destoryCluster()
    latch.await()
  }

  /**
   * 从本地路由表中获取服务地址
   * @param method 资源操作方式
   * @param path 原始的请求地址
   * @return 正则化处理后的地址
   */
  protected def getAddress(method: String, path: String): String = {
    val pathWithoutParameter = removeParameter(path)
    var nPath = if (EventBusOptions.localAddressPathMap.contains(pathWithoutParameter)) pathWithoutParameter else null
    if (nPath == null) {
      EventBusOptions.localRAddressPathList.foreach {
        pattern =>
          val matcher = pattern.matcher(pathWithoutParameter)
          if (matcher.matches()) {
            //找到正则的地址，使用正则化处理后的地址代替原始地址
            nPath = method + "__" + pattern.pattern()
          }
      }
    } else {
      //找到非正则的地址
      nPath = method + "__" + pathWithoutParameter
    }
    nPath
  }

  /**
   * 删除请求的Path参数
   *
   * @param path 请求Path
   * @return 不带参数的请求Path
   */
  protected def removeParameter(path: String): String = {
    if (path.contains("?")) {
      path.substring(0, path.indexOf("?"))
    } else {
      path
    }
  }

  /* protected def unRegisterAddress(address: String, useRegex: Boolean): Unit = {
     registerAddressMap.remove(if (useRegex) "__r_address__" else "__address__", address, new Handler[AsyncResult[java.lang.Boolean]] {
       override def handle(event: AsyncResult[java.lang.Boolean]): Unit = {
         if (!event.succeeded()) {
           logger.error(s"UnRegister $address to FLAG_REGISTER_ADDRESS failed!", event.cause())
         }
       }
     })
   }*/

  protected def addAddress(method: String, path: String): String = {
    method + "__" + path
  }

  /**
   * 注册服务地址到集群
   * @param address 正则化处理后的服务地址
   * @param isRegex 是否是正则
   */
  protected def registerAddressPathToCluster(address: String, isRegex: Boolean): Unit = {
    EventBusOptions.clusterAddressPathMap.add(if (isRegex) "__r_address_path__" else "__address_path__", address, new Handler[AsyncResult[Void]] {
      override def handle(event: AsyncResult[Void]): Unit = {
        if (event.succeeded()) {
          //成功后通知所有活动节点更新各自的本地路由表
          EventBusOptions.eb.publish("__ADD_ADDRESS_PATH__", new JsonObject().put("isRegex", isRegex).put("address", address))
          logger.debug(s"Register $address to REGISTER_MAP_ADDRESS_PATH success.")
        } else {
          logger.error(s"Register $address to REGISTER_MAP_ADDRESS_PATH failed!", event.cause())
        }
      }
    })
  }

  /**
   * 获取请求Path的参数
   *
   * @param path 请求Path
   * @return 参数集合
   */
  protected def getParameter(path: String): Map[String, String] = {
    val parameter: Map[String, String] = if (path.indexOf("?") != -1) {
      val param = collection.mutable.Map[String, String]()
      path.substring(path.indexOf("?") + 1).split("&").map {
        item =>
          val t = item.split("=")
          if (t.size == 2) {
            param += (t(0) -> t(1))
          } else {
            param += (t(0) -> "")
          }
      }
      param.toMap
    } else {
      Map[String, String]()
    }
    parameter
  }

}

object EventBusOptions extends LazyLogging {

  val localAddressPathMap = collection.mutable.Map[String, Void]()
  val localRAddressPathList = ArrayBuffer[Pattern]()
  private val existInstance = new AtomicBoolean(false)
  var eb: EventBus = _
  /**
   * 集群中的路由映射表
   * <p>
   * key = __r_address_path__ 或 __r_address_path__ 分别表示 正则地址和非正则地址，value = 正则化处理后的地址列表（MultiMap类型，value是List）
   */
  var clusterAddressPathMap: AsyncMultiMap[String, String] = _
  private var startFinish = false

}
