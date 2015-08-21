package com.ecfront.ez.framework.rpc.process

import java.io.File

import com.ecfront.ez.framework.rpc.{Router, Server}

/**
 * 服务处理器
 */
trait ServerProcessor extends Processor {

  protected var router: Router = _
  protected var rootUploadPath: String = _
  protected var rpcServer: Server = _

  private[rpc] def init(port: Int, host: String, router: Router, rootUploadPath: String, rpcServer: Server) {
    this.port = port
    this.host = host
    this.router = router
    this.rootUploadPath = rootUploadPath
    if (!this.rootUploadPath.endsWith(File.separator)) {
      this.rootUploadPath += File.separator
    }
    this.rpcServer = rpcServer
    init()
  }

  /**
   * 注册到路由表后的调用方法
   * @param method  资源操作方式
   * @param path 资源路径，正则化处理
   * @param isRegex 是否是正则规则
   */
  protected[rpc] def process(method: String, path: String, isRegex: Boolean): Unit

}
