package com.ecfront.ez.framework.service.rpc.foundation

/**
  * 重定向VO
  * @param url 目标URL
  */
case class RespRedirect(url:String)

/**
  * 返回原始信息 VO，不做 Resp封装
  * @param raw 原始信息
  */
case class Raw(raw:Any)
