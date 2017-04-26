package com.ecfront.ez.framework.core.rpc

import java.io.File
import java.util.Date

import scala.beans.BeanProperty

class APIDTO {

  @Label("方法")
  @BeanProperty var method: String = _
  @Label("路径")
  @BeanProperty var path: String = _

}

object APIDTO {

  def apply(method: String, path: String): APIDTO = {
    val dto = new APIDTO()
    dto.method = method
    dto.path = path
    dto
  }
}

object Method extends Enumeration {
  type Method = Value
  val GET, POST, PUT, DELETE, PUB_SUB, REQ_RESP, ACK, WS = Value
}


/**
  * Opt Info VO
  *
  * @param token                token
  * @param accountCode          code
  * @param loginId             登录id
  * @param name                 姓名
  * @param email                email
  * @param image                头像
  * @param organizationCode     组织编码
  * @param organizationName     组织名称
  * @param organizationCategory 组织类型
  * @param roleCodes            角色编码列表
  * @param extInfo              扩展信息
  */
case class OptInfo(
                  @Label("token")  token: String,
                  @Label("账号编码") accountCode: String,
                  @Label("登录Id")  loginId: String,
                  @Label("姓名")  name: String,
                  @Label("email")  email: String,
                  @Label("头像")  image: String,
                  @Label("组织编码")  organizationCode: String,
                  @Label("组织名称")  organizationName: String,
                  @Label("组织类型")  organizationCategory: String,
                  @Label("角色编码列表")  roleCodes: Set[String],
                  @Label("最后一次登录时间")  lastLoginTime: Date,
                  @Label("扩展Id")  extId: String,
                  @Label("扩展信息")  extInfo: String)

/**
  * 重定向VO
  *
  * @param url 目标URL
  */
case class RespRedirect(url: String)

/**
  * 返回原始信息 VO，不做 Resp封装
  *
  * @param raw 原始信息
  */
case class Raw(raw: Any)

/**
  * 文件上传请求
  *
  * @param file      要上传的文件
  * @param fieldName 对应的field名称
  * @param fileName  生成的文件名称
  */
case class ReqFile(file: File, fieldName: String, fileName: String = null)

/**
  * 指定特殊文件名的下载文件VO
  *
  * @param file     要下载的文件
  * @param fileName 文件名称
  */
case class DownloadFile(file: String, fileName: String)

object DownloadFile {
  def apply(file: File): DownloadFile = {
    new DownloadFile(file.getPath, file.getName)
  }
}






