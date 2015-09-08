package com.ecfront.ez.framework.rpc.process

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.rpc.Client
import com.fasterxml.jackson.databind.node.TextNode
import io.vertx.core.Handler

import scala.concurrent.{Future, Promise}

/**
 * 连接处理器
 */
trait ClientProcessor extends Processor {

  /**
   * 是否使用point to point 方式
   */
  private[rpc] var isPointToPoint = true
  private[rpc] var rpcClient: Client = _

  /**
   * 处理Result包装返回类型（异步方式）
   * @param method  资源操作方式
   * @param path 资源路径
   * @param requestBody  请求对象
   * @param responseClass 返回对象的类型
   * @param fun 业务方法
   * @param inject 注入信息
   * @return Result包装对象
   */
  protected[rpc] def process[E](method: String, path: String, requestBody: Any, responseClass: Class[E], fun: => Resp[E] => Unit, inject: Any) {
    doProcess[E, Resp[E]](method, path, requestBody, responseClass, classOf[Resp[E]], {
      result =>
        if (result.isDefined) {
          fun(result.get)
        }
    }, inject)
  }

  /**
   * 处理Result包装返回类型（同步方式）
   * @param method  资源操作方式
   * @param path 资源路径
   * @param requestBody  请求对象
   * @param responseClass 返回对象的类型
   * @param inject 注入信息
   * @return Result包装对象
   */
  protected[rpc] def process[E](method: String, path: String, requestBody: Any, responseClass: Class[E], inject: Any): Future[Option[Resp[E]]] = {
    val p = Promise[Option[Resp[E]]]()
    doProcess[E, Resp[E]](method, path, requestBody, responseClass, classOf[Resp[E]], {
      result =>
        p.success(result)
    }, inject)
    p.future
  }

  /**
   * 处理原生返回类型（异步方式）
   * @param method  资源操作方式
   * @param path 资源路径
   * @param requestBody  请求对象
   * @param responseClass 返回对象的类型
   * @param fun 业务方法
   * @param inject 注入信息
   * @return 原生对象
   */
  protected[rpc] def processRaw[E](method: String, path: String, requestBody: Any, responseClass: Class[E], fun: => E => Unit, inject: Any) {
    doProcess[E, E](method, path, requestBody, responseClass, responseClass, {
      result =>
        if (result.isDefined) {
          fun(result.get)
        }
    }, inject)
  }

  /**
   * 处理原生返回类型（同步方式）
   * @param method  资源操作方式
   * @param path 资源路径
   * @param requestBody  请求对象
   * @param responseClass 返回对象的类型
   * @param inject 注入信息
   * @return 原生对象
   */
  protected[rpc] def processRaw[E](method: String, path: String, requestBody: Any, responseClass: Class[E], inject: Any): Future[Option[E]] = {
    val p = Promise[Option[E]]()
    doProcess[E, E](method, path, requestBody, responseClass, responseClass, {
      result =>
        p.success(result)
    }, inject)
    p.future
  }

  /**
   * 实际处理方法
   * @param method  资源操作方式
   * @param path 资源路径
   * @param requestBody  请求对象
   * @param responseClass 返回对象的类型
   * @param resultClass 返回的结果类型（可能被Resp包装）
   * @param finishFun 完成处理方法
   * @param inject 注入信息
   * @tparam E  返回对象的类型
   * @tparam F 返回的结果类型（可能被Resp包装）
   */
  protected def doProcess[E, F](method: String, path: String, requestBody: Any, responseClass: Class[E], resultClass: Class[F], finishFun: => Option[F] => Unit, inject: Any)

  /**
   * 解析服务器返回的结果
   * @param result 服务器返回的结果，字符串格式
   * @param responseClass 返回的Body类型
   * @return 包装后的Resp返回结果
   */
  protected def parseResp[E](result: String, responseClass: Class[E]): Resp[E] = {
    val json = JsonHelper.toJson(result)
    val code = json.get(Resp.CODE).asText()
    val body = json.get(Resp.BODY) match {
      case b: TextNode => JsonHelper.toObject(b.asText(), responseClass)
      case _ => JsonHelper.toObject(json.get(Resp.BODY), responseClass)
    }
    if (code == StandardCode.SUCCESS) {
      Resp.success(body)
    } else {
      JsonHelper.toObject(json, classOf[Resp[E]])
    }
  }

  protected def clientExecute[F](method: String, tPath: String, finishFun: => (Option[F]) => Unit, result: Option[F]) {
    vertx.executeBlocking(new Handler[io.vertx.core.Future[Any]] {
      override def handle(future: io.vertx.core.Future[Any]): Unit = {
        try {
          finishFun(result)
          rpcClient.postExecuteInterceptor(method, tPath)
        }
        catch {
          case e: Exception =>
            logger.error("Execute function error.", e)
        }
        future.complete()
      }
    }, false, new Handler[io.vertx.core.AsyncResult[Any]] {
      override def handle(event: io.vertx.core.AsyncResult[Any]): Unit = {
      }
    })
  }

  private[rpc] def init(port: Int, host: String, client: Client) {
    this.port = port
    this.host = host
    this.rpcClient = client
    init()
  }

}
