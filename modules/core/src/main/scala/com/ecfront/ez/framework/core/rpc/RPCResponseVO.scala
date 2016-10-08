package com.ecfront.ez.framework.core.rpc

import java.io.File

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
case class ReqFile(file: File, fieldName: String, fileName: String=null)

/**
  * 指定特殊文件名的下载文件VO
  *
  * @param file      要下载的文件
  * @param fileName  文件名称
  */
case class DownloadFile(file: String, fileName: String)
