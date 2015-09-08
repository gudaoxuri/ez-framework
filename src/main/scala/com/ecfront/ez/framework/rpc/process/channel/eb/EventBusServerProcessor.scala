package com.ecfront.ez.framework.rpc.process.channel.eb

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.rpc.cluster.ClusterManager
import com.ecfront.ez.framework.rpc.process.ServerProcessor
import io.vertx.core.eventbus.Message
import io.vertx.core.{AsyncResult, Future, Handler}

import scala.collection.JavaConversions._

/**
 * EventBus 服务处理器
 */
class EventBusServerProcessor extends ServerProcessor with EventBusOptions {

  override protected def init(): Unit = {
    initEventbus(host, port)
  }

  override protected[rpc] def process(method: String, path: String, isRegex: Boolean): Unit = {
    val address = addAddress(method, path)
    //注册正则化后的地址
    EventBusOptions.eb.consumer(address)
      .handler(new Handler[Message[String]] {
        override def handle(event: Message[String]): Unit = {
          val method = event.headers().get(FLAG_METHOD)
          val path = event.headers().get(FLAG_PATH)
          val isReply = event.headers().get(FLAG_IS_REPLY)
          val parameters = collection.mutable.Map[String, String]()
          val interceptInfo = collection.mutable.Map[String, String]()
          event.headers().foreach {
            item =>
              //排除框架定义的变量
              if (!item.getKey.startsWith("__") && !item.getKey.endsWith("__")) {
                parameters += item.getKey -> item.getValue
              } else if (item.getKey.startsWith(FLAG_INTERCEPTOR_INFO)) {
                interceptInfo += item.getKey.substring(FLAG_INTERCEPTOR_INFO.length) -> item.getValue
              }
          }
          val (preResult, fun, newParameters, postFun) = router.getFunction(method, path, parameters.toMap, interceptInfo.toMap)
          if (preResult) {
            val body = event.body()
            ClusterManager.vertx.executeBlocking(new Handler[Future[Any]] {
              override def handle(future: Future[Any]): Unit = {
                try {
                  val result =
                    if (fun != null) {
                      postFun(fun.execute(newParameters, if (body == null) null else JsonHelper.toObject(body, fun.requestClass), preResult.body))
                    } else {
                      rpcServer.any(method, path, parameters.toMap, body, preResult.body)
                    }
                  if (isReply == "1") {
                    event.reply(result match {
                      case r: String => r
                      case _ => JsonHelper.toJsonString(result)
                    })
                  }
                }
                catch {
                  case e: Exception =>
                    logger.error("Execute function error.", e)
                    if (isReply == "1") {
                      event.reply(JsonHelper.toJsonString(Resp.serverError(e.getMessage)))
                    }
                }
                future.complete()
              }
            }, false, new Handler[AsyncResult[Any]] {
              override def handle(event: AsyncResult[Any]): Unit = {
              }
            })
          } else {
            if (isReply == "1") {
              event.reply(JsonHelper.toJsonString(preResult))
            }
          }
        }
      }).completionHandler(new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          //注册成功后通知各个节点更新本地路由表
          registerAddressPathToCluster(path, isRegex)
          logger.debug(s"Register $address to local success.")
        } else {
          logger.error(s"Register $address to local failed.", res.cause())
        }
      }
    })
  }

  override private[rpc] def destroy(): Unit = {
    destoryEventbus()
  }

}

