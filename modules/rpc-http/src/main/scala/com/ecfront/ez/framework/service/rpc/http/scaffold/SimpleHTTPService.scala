package com.ecfront.ez.framework.service.rpc.http.scaffold

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, StandardCopyOption}
import java.util.Date

import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.rpc.foundation.scaffold.SimpleRPCService
import com.ecfront.ez.framework.service.rpc.http.{FileType, ServiceAdapter}
import com.ecfront.ez.framework.service.storage.foundation._

trait SimpleHttpService[M <: BaseModel, C <: EZRPCContext] extends SimpleRPCService[M, C] {

  protected var baseUri = BeanHelper.getClassAnnotation[RPC](this.getClass).get.baseUri
  baseUri = baseUri.substring(1)
  if (!baseUri.endsWith("/")) {
    baseUri += "/"
  }

  protected def allowUpload = true

  protected def allowExport = true

  protected def allowUploadTypes = List(FileType.TYPE_COMPRESS, FileType.TYPE_IMAGE, FileType.TYPE_OFFICE)

  protected def allowExportFields = BeanHelper.findFields(modelClazz).keys.toList

  val resName = modelClazz.getSimpleName.toLowerCase

  @POST("res/")
  def rpcResUpload(parameter: Map[String, String], fileName: String, context: C): Resp[String] = {
    logger.trace(s" RPC simple upload : $parameter")
    val tmpFile = new File(ServiceAdapter.resourcePath + fileName)
    if (allowUpload) {
      val contextType = Files.probeContentType(tmpFile.toPath)
      if (allowUploadTypes.flatten(FileType.types(_)).contains(contextType)) {
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

  @GET("export/")
  def rpcExport(parameter: Map[String, String], context: C): Resp[File] = {
    logger.trace(s" RPC simple export : $parameter")
    if (allowExport) {
      val condition = if (parameter.contains("condition")) parameter("condition") else ""
      val res = storageObj.find(condition, List(), context)
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
      Resp.notImplemented("")
    }
  }

}
