package com.ecfront.ez.framework.service.rpc.http.scaffold

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, StandardCopyOption}
import java.util.Date

import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import com.ecfront.ez.framework.core.helper.{FileType, TimeHelper}
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.rpc.foundation.scaffold.SimpleRPCService
import com.ecfront.ez.framework.service.rpc.http.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._

/**
  * 简单HTTP服务
  *
  * @tparam M 对应的实体类型
  * @tparam C 对应的RPC上下文
  */
trait SimpleHTTPService[M <: BaseModel, C <: EZRPCContext] extends SimpleRPCService[M, C] {

  protected var baseUri = BeanHelper.getClassAnnotation[RPC](this.getClass).get.baseUri
  baseUri = baseUri.substring(1)
  if (!baseUri.endsWith("/")) {
    baseUri += "/"
  }

  /**
    * 是否允许上传操作
    *
    * @return 是否允许上传操作
    */
  protected def allowUpload = true

  /**
    * 是否允许导出操作
    *
    * @return 是否允许导出操作
    */
  protected def allowExport = true

  /**
    * 上传类型限定
    *
    * @return 允许的类型
    */
  protected def allowUploadTypes = List(FileType.TYPE_COMPRESS, FileType.TYPE_IMAGE, FileType.TYPE_OFFICE)

  /**
    * 导出字段限定
    *
    * @return 允许导出的字段
    */
  protected def allowExportFields = BeanHelper.findFields(modelClazz).keys.toList

  val resName = modelClazz.getSimpleName.toLowerCase

  /**
    * 上传操作
    *
    * 按 资源类型+时间戳+文件名 存储
    *
    * @param parameter 请求参数，此处暂无作用
    * @param fileName  默认保存文件名
    * @param context   PRC上下文
    * @return 下载URL
    */
  @POST("res/")
  def rpcResUpload(parameter: Map[String, String], fileName: String, context: C): Resp[String] = {
    logger.trace(s" RPC simple upload : $parameter")
    val tmpFile = new File(ServiceAdapter.resourcePath + fileName)
    if (allowUpload) {
      val contextType = Files.probeContentType(tmpFile.toPath)
      if (allowUploadTypes.flatten.contains(contextType)) {
        val createDate = TimeHelper.df.format(new Date())
        val path = ServiceAdapter.resourcePath + resName + File.separator + createDate + File.separator
        if (!new File(path).exists()) {
          new File(path).mkdirs()
        }
        Files.move(tmpFile.toPath,
          new File(path + fileName).toPath,
          StandardCopyOption.ATOMIC_MOVE)
        Resp.success(baseUri + "res/" + createDate + "/" + fileName)
      } else {
        tmpFile.delete()
        Resp.badRequest(s"Request Content-type [$contextType] not allow")
      }
    } else {
      tmpFile.delete()
      Resp.notImplemented("")
    }
  }

  /**
    * 下载操作
    *
    * @param parameter 请求参数
    * @param context   PRC上下文
    * @return 要下载的资源
    */
  @GET("res/:date/:fileName")
  def rpcResGet(parameter: Map[String, String], context: C): Resp[File] = {
    logger.trace(s" RPC simple download : $parameter")
    if (allowUpload) {
      val file = new File(ServiceAdapter.resourcePath + resName + File.separator + parameter("date") + File.separator + parameter("fileName"))
      if (file.exists()) {
        Resp.success(file)
      } else {
        Resp.notFound(s"Resource：${file.getPath}")
      }
    } else {
      Resp.notImplemented("")
    }
  }

  /**
    * 导出操作
    *
    * @param parameter 请求参数，可以包含`condition` 用于筛选条件
    * @param context   PRC上下文
    * @return 导出文件
    */
  @GET("export/")
  def rpcExport(parameter: Map[String, String], context: C): Resp[File] = {
    logger.trace(s" RPC simple export : $parameter")
    if (allowExport) {
      val conditionR = if (parameter.contains("condition")){
        conditionCheck(parameter("condition"))
      } else{
        Resp.success("")
      }
      if (conditionR) {
        val res = storageObj.find(conditionR.body, List(), context.toStorageContext)
        var file: File = null
        var bw: BufferedWriter = null
        try {
          file = File.createTempFile("export", ".csv")
          file.deleteOnExit()
          bw = new BufferedWriter(new FileWriter(file, true))
          bw.write(allowExportFields.mkString(",") + "\r\n")
          val lines = JsonHelper.toJson(res.body).iterator()
          while (lines.hasNext) {
            val line = lines.next()
            bw.write(allowExportFields.map {
              f =>
                line.get(f).asText()
            }.mkString(",") + "\r\n")
          }
          bw.close()
          Resp.success(file)
        } catch {
          case e: Throwable =>
            bw.close()
            Resp.serverError(s"File create error : ${e.getMessage}")
        }
      } else {
        conditionR
      }
    } else {
      Resp.notImplemented("")
    }
  }

}
