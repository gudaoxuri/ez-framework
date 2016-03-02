package com.asto.ez.framework.scaffold

import java.io.{BufferedWriter, File, FileWriter}
import java.lang.reflect.ParameterizedType
import java.nio.file.{Files, StandardCopyOption}
import java.util.Date

import com.asto.ez.framework.helper.TimeHelper
import com.asto.ez.framework.rpc._
import com.asto.ez.framework.storage._
import com.asto.ez.framework.storage.jdbc.JDBCBaseStorage
import com.asto.ez.framework.{EZContext, EZGlobal}
import com.ecfront.common.{AsyncResp, BeanHelper, JsonHelper}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global

trait SimpleRPCService[M <: BaseModel] extends LazyLogging {

  protected val storageObj: BaseStorage[M]
  protected var baseUri = BeanHelper.getClassAnnotation[RPC](this.getClass).get.baseUri
  baseUri = baseUri.substring(1)
  if (!baseUri.endsWith("/")) {
    baseUri += "/"
  }

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  protected val _isJDBCModel = storageObj.isInstanceOf[JDBCBaseStorage[_]]
  protected val modelObj = _modelClazz.newInstance()
  protected val _emptyCondition = if (_isJDBCModel) "1=1" else "{}"

  protected def allowUpload = true

  protected def allowExport = true

  protected def allowUploadTypes = List(FileType.TYPE_COMPRESS, FileType.TYPE_IMAGE, FileType.TYPE_OFFICE)

  protected def allowExportFields = BeanHelper.findFields(_modelClazz).keys.toList

  @POST("")
  def _rpc_save(parameter: Map[String, String], body: String, p: AsyncResp[String], context: EZContext): Unit = {
    logger.trace(s" RPC simple save : $body")
    try {
      val model = JsonHelper.toObject(body, _modelClazz)
      storageObj.save(model, context).onSuccess {
        case resp => p.resp(resp)
      }
    } catch {
      case e: Throwable =>
        logger.error("RPC simple save error : " + e.getMessage, e)
        p.badRequest("RPC simple save error : " + e.getMessage)
    }
  }

  @PUT(":id/")
  def _rpc_update(parameter: Map[String, String], body: String, p: AsyncResp[String], context: EZContext): Unit = {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
    } else {
      logger.trace(s" RPC simple update : $body")
      try {
        val model = JsonHelper.toObject(body, _modelClazz)
        storageObj.update(model, context).onSuccess {
          case resp => p.resp(resp)
        }
      } catch {
        case e: Throwable =>
          logger.error("RPC simple save error : " + e.getMessage, e)
          p.badRequest("RPC simple save error : " + e.getMessage)
      }
    }
  }

  @GET("enable/")
  def _rpc_findEnable(parameter: Map[String, String], p: AsyncResp[List[M]], context: EZContext): Unit = {
    logger.trace(s" RPC simple find enable : $parameter")
    if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
      val condition = if (parameter.contains("condition")) parameter("condition") else _emptyCondition
      storageObj.asInstanceOf[StatusStorage[_]].findEnabled(condition, List(), context).onSuccess {
        case resp => p.resp(resp)
      }
    } else {
      p.notImplemented("启用方法未实现")
    }
  }

  @GET("")
  def _rpc_find(parameter: Map[String, String], p: AsyncResp[List[M]], context: EZContext): Unit = {
    logger.trace(s" RPC simple find : $parameter")
    val condition = if (parameter.contains("condition")) parameter("condition") else _emptyCondition
    storageObj.find(condition, List(), context).onSuccess {
      case resp => p.resp(resp)
    }
  }


  @GET("page/:pageNumber/:pageSize/")
  def _rpc_page(parameter: Map[String, String], p: AsyncResp[Page[M]], context: EZContext): Unit = {
    logger.trace(s" RPC simple page : $parameter")
    val condition = if (parameter.contains("condition")) parameter("condition") else _emptyCondition
    val pageNumber = if (parameter.contains("pageNumber")) parameter("pageNumber").toLong else 1L
    val pageSize = if (parameter.contains("pageSize")) parameter("pageSize").toInt else 10
    storageObj.page(condition, List(), pageNumber, pageSize, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  @GET(":id/")
  def _rpc_get(parameter: Map[String, String], p: AsyncResp[M], context: EZContext): Unit = {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
    } else {
      val id = parameter("id")
      logger.trace(s" RPC simple get : $id")
      storageObj.getById(id, context).onSuccess {
        case resp => p.resp(resp)
      }
    }
  }

  @DELETE(":id/")
  def _rpc_delete(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
    } else {
      val id = parameter("id")
      logger.trace(s" RPC simple delete : $id")
      storageObj.deleteById(id, context).onSuccess {
        case resp => p.resp(resp)
      }
    }
  }

  @GET(":id/enable/")
  def _rpc_enable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
    } else {
      if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
        val id = parameter("id")
        logger.trace(s" RPC simple enable : $id")
        storageObj.asInstanceOf[StatusStorage[_]].enableById(id, context).onSuccess {
          case resp => p.resp(resp)
        }
      } else {
        p.notImplemented("启用方法未实现")
      }
    }
  }

  @GET(":id/disable/")
  def _rpc_disable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
    } else {
      if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
        val id = parameter("id")
        logger.trace(s" RPC simple enable : $id")
        storageObj.asInstanceOf[StatusStorage[_]].disableById(id, context).onSuccess {
          case resp => p.resp(resp)
        }
      } else {
        p.notImplemented("停用方法未实现")
      }
    }
  }

  val resName = _modelClazz.getSimpleName.toLowerCase

  @POST("res/")
  def _rpc_res_upload(parameter: Map[String, String], fileName: String, p: AsyncResp[String], context: EZContext): Unit = {
    logger.trace(s" RPC simple upload : $parameter")
    val tmpFile = new File(EZGlobal.ez_rpc_http_resource_path + fileName)
    if (allowUpload) {
      val contextType = Files.probeContentType(tmpFile.toPath)
      if (allowUploadTypes.flatten(FileType.types(_)).contains(contextType)) {
        val createDate = TimeHelper.df.format(new Date())
        val path = EZGlobal.ez_rpc_http_resource_path + resName + File.separator + createDate + File.separator
        if (!new File(path).exists()) {
          new File(path).mkdirs()
        }
        Files.move(tmpFile.toPath,
          new File(path + fileName).toPath,
          StandardCopyOption.ATOMIC_MOVE)
        p.success(baseUri + "res/" + createDate + "/" + fileName)
      } else {
        tmpFile.delete()
        p.badRequest(s"请求类型[$contextType]不允许")
      }
    } else {
      tmpFile.delete()
      p.notImplemented("此方法未实现")
    }
  }

  @GET("res/:date/:fileName")
  def _rpc_res_get(parameter: Map[String, String], p: AsyncResp[File], context: EZContext): Unit = {
    logger.trace(s" RPC simple download : $parameter")
    if (allowUpload) {
      val file = new File(EZGlobal.ez_rpc_http_resource_path + resName + File.separator + parameter("date") + File.separator + parameter("fileName"))
      if (file.exists()) {
        p.success(file)
      } else {
        p.notFound(s"找不到此资源：${file.getPath}")
      }
    } else {
      p.notImplemented("此方法未实现")
    }
  }

  @GET("export/")
  def _rpc_export(parameter: Map[String, String], p: AsyncResp[File], context: EZContext): Unit = {
    logger.trace(s" RPC simple export : $parameter")
    if (allowExport) {
      val condition = if (parameter.contains("condition")) parameter("condition") else _emptyCondition
      storageObj.find(condition, List(), context).onSuccess {
        case resp =>
          var file: File = null
          var bw: BufferedWriter = null
          try {
            file = File.createTempFile("export", ".csv")
            file.deleteOnExit()
            bw = new BufferedWriter(new FileWriter(file, true))
            bw.write(allowExportFields.mkString(",") + "\r\n")
            val lines = JsonHelper.toJson(resp.body).iterator()
            while (lines.hasNext) {
              val line = lines.next()
              bw.write(allowExportFields.map {
                f =>
                  line.get(f).asText()
              }.mkString(",") + "\r\n")
            }
            bw.close()
            p.success(file)
          } catch {
            case e: Throwable =>
              bw.close()
              logger.error("文件生成错误", e)
              p.serverError("文件生成错误")
          }
      }
    } else {
      p.notImplemented("此方法未实现")
    }
  }

}
