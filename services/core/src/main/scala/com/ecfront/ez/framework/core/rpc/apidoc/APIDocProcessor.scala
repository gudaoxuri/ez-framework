package com.ecfront.ez.framework.core.rpc.apidoc

import java.io.{File, FileWriter}

import com.ecfront.common.BeanHelper
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.{Label, Require}

import scala.collection.mutable.ArrayBuffer

object APIDocProcessor extends Logging {

  private var path: String = _

  def init(_path: String): Unit = {
    path = if (_path != null) _path else "/tmp/docs/"
    logger.info("API Doc path is :" + path)
    val p = new File(path)
    if (!p.exists()) {
      p.mkdirs()
    }
  }

  def build(apiDoc: APIDocSectionVO): Unit = {
    val items = apiDoc.items.reverse.map {
      item =>
        val reqBody = packageReqBody(item)
        val respBody = packageRespBody(item)
        s"==== ${item.name}\r\n${item.desc.split("\r\n").map(_.trim).mkString("\r\n")}\r\n\r\n*请求*\r\n\r\n [${item.method}] ${item.uri}\r\n$reqBody\r\n\r\n*响应*\r\n\r\n$respBody\r\n"
    }.mkString("\r\n")
    if (items.nonEmpty) {
      val data = s"=== ${apiDoc.name}${apiDoc.desc.split("\r\n").map(_.trim).mkString("\r\n")}\r\n$items"
      val file = new File(path + (if (apiDoc.fileName.endsWith("$")) apiDoc.fileName.substring(0, apiDoc.fileName.length - 1) else apiDoc.fileName) + ".adoc")
      if (file.exists()) {
        file.delete()
      }
      val fw = new FileWriter(file)
      try {
        fw.write(data)
      } catch {
        case e: Throwable =>
          throw e
      } finally {
        fw.close()
      }
    }
  }

  private def packageRespBody(item: APIDocItemVO): String = {
    val respStr = item.respStr
    if (!respStr.startsWith("com.ecfront.common.Resp")) {
      doPackageRespBody("", item.respExt)
    } else {
      val respBodyStr = respStr.substring(respStr.indexOf("[") + 1, respStr.length - 1)
      if (respBodyStr.contains("[")) {
        if (respBodyStr.startsWith("List") || respBodyStr.startsWith("Set") || respBodyStr.startsWith("Seq") || respBodyStr.startsWith("Vector") || respBodyStr.startsWith("Array")) {
          doPackageRespBody(respBodyStr.substring(respBodyStr.indexOf("[") + 1, respBodyStr.length - 1), item.respExt, isList = true)
        } else if (respBodyStr.startsWith("com.ecfront.ez.framework.service.jdbc.Page")) {
          doPackageRespBody(respBodyStr.substring(respBodyStr.indexOf("[") + 1, respBodyStr.length - 1), item.respExt, isPage = true)
        } else {
          doPackageRespBody("", item.respExt)
        }
      } else if (respBodyStr != "Void") {
        doPackageRespBody(respBodyStr, item.respExt)
      } else {
        "_无_"
      }
    }
  }

  private def doPackageRespBody(bodyStr: String, respExt: String, isList: Boolean = false, isPage: Boolean = false): String = {
    val extFieldNames = respExt.split("\r\n").map(_.split("\\|")).filter(_.length == 4).map(_ (1))
    val body = ArrayBuffer[String]()
    body ++= Seq("|===","""|列名|类型|说明""", "")
    if (bodyStr.nonEmpty && bodyStr.contains(".")) {
      val clazz = Class.forName(bodyStr)
      val fieldsInfo = BeanHelper.findFields(clazz)
      val labelsInfo = BeanHelper.findFieldAnnotations(clazz).filter {
        ann =>
          ann.annotation.isInstanceOf[Label] || ann.annotation.getClass.getCanonicalName == "com.ecfront.ez.framework.service.jdbc.Desc"
      }
      if (isPage) {
        body +="""|pageNumber|Long|当前页，从1开始  """
        body +="""|pageSize|Int|每页条数  """
        body +="""|pageTotal|Long|总共页数  """
        body +="""|recordTotal|Long|总共记录数  """
        body +="""|objects|Array|当前页的实体列表  """
      }
      if (isList) {
        body +="""|Array |   | """
      }
      body ++= labelsInfo.filterNot(i => extFieldNames.contains(i.fieldName)).map {
        labelInfo =>
          val fieldName = labelInfo.fieldName
          val labelName =
            if (labelInfo.annotation.isInstanceOf[Label]) {
              labelInfo.annotation.asInstanceOf[Label].label
            } else {
              BeanHelper.getValue(labelInfo.annotation.asInstanceOf[AnyRef], "label").get.asInstanceOf[String]
            }
          val dType = fieldsInfo(labelInfo.fieldName)
          s"""|${if (isList || isPage) "-" else ""}$fieldName|$dType|${labelName.x}"""
      }
    }
    body.mkString("\r\n") + (if (respExt.trim.nonEmpty) s"\r\n${respExt.split("\r\n").map(_.trim).mkString("\n")}\r\n|===" else "\r\n|===")
  }

  private def packageReqBody(item: APIDocItemVO): String = {
    if (item.reqbody == null) ""
    else {
      val extFieldNames = item.reqExt.split("\r\n").map(_.split("\\|")).filter(_.length == 5).map(_ (1))
      val fieldsInfo = BeanHelper.findFields(item.reqbody)
      val labelsInfo = BeanHelper.findFieldAnnotations(item.reqbody).filter {
        ann =>
          ann.annotation.isInstanceOf[Label] || ann.annotation.getClass.getCanonicalName == "com.ecfront.ez.framework.service.jdbc.Desc"
      }
      val requiresInfo = BeanHelper.findFieldAnnotations(item.reqbody, Seq(classOf[Require]))
      val body = Seq("|===",s"""|列名|类型|说明|是否必填""", "") ++
        labelsInfo.filterNot(i => extFieldNames.contains(i.fieldName)).map {
          labelInfo =>
            val fieldName = labelInfo.fieldName
            val labelName =
              if (labelInfo.annotation.isInstanceOf[Label]) {
                labelInfo.annotation.asInstanceOf[Label].label
              } else {
                BeanHelper.getValue(labelInfo.annotation.asInstanceOf[AnyRef], "label").get.asInstanceOf[String]
              }
            val isRequire = requiresInfo.exists(_.fieldName == fieldName)
            val dType = fieldsInfo(labelInfo.fieldName)
            s"""|$fieldName|$dType|${labelName.x}|$isRequire"""
        }
      body.mkString("\r\n") + (if (item.reqExt.trim.nonEmpty) s"\r\n${item.reqExt.split("\r\n").map(_.trim).mkString("\r\n")}\r\n|===" else "\r\n|===")
    }
  }
}
