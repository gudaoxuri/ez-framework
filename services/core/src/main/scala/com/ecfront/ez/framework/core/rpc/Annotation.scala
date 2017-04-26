package com.ecfront.ez.framework.core.rpc

import scala.annotation.StaticAnnotation


/**
  * 标记需要注册RPC的类，用于Auto Building特性
  *
  * @param baseUri 此类的根路径
  */
case class RPC(baseUri: String, docName: String, docDesc: String) extends StaticAnnotation

/**
  * 注册用于接收HTTP(s) Get请求，用于Auto Building特性
  *
  * @param uri 请求路径（http:get:baseUri+uri）
  */
case class GET(uri: String, docName: String, docDesc: String, docRespExt: String) extends StaticAnnotation

/**
  * 注册用于接收HTTP(s) Post请求，用于Auto Building特性
  *
  * @param uri 请求路径（http:post:baseUri+uri）
  */
case class POST(uri: String, docName: String, docDesc: String, docReqExt: String, docRespExt: String) extends StaticAnnotation

/**
  * 注册用于接收HTTP(s) Put请求，用于Auto Building特性
  *
  * @param uri 请求路径（http:put:baseUri+uri）
  */
case class PUT(uri: String, docName: String, docDesc: String, docReqExt: String, docRespExt: String) extends StaticAnnotation

/**
  * 注册用于接收HTTP(s) Delete请求，用于Auto Building特性
  *
  * @param uri 请求路径（http:delete:baseUri+uri）
  */
case class DELETE(uri: String, docName: String, docDesc: String, docRespExt: String) extends StaticAnnotation

/**
  * 注册用于接收WebSocket请求，用于Auto Building特性
  *
  * @param uri 请求路径（ws:baseUri+uri）
  */
case class WS(uri: String, docName: String, docDesc: String, docReqExt: String, docRespExt: String) extends StaticAnnotation

/**
  * 注册用于接收Publish请求，用于Auto Building特性
  *
  * @param uri 请求路径（baseUri+uri）
  */
case class SUB(uri: String, docName: String, docDesc: String, docReqExt: String, docRespExt: String) extends StaticAnnotation

/**
  * 注册用于接收Request请求，用于Auto Building特性
  *
  * @param uri 请求路径（baseUri+uri）
  */
case class RESP(uri: String, docName: String, docDesc: String, docReqExt: String, docRespExt: String) extends StaticAnnotation

/**
  * 注册用于接收ACK请求，用于Auto Building特性
  *
  * @param uri 请求路径（baseUri+uri）
  */
case class REPLY(uri: String, docName: String, docDesc: String, docReqExt: String, docRespExt: String) extends StaticAnnotation

/**
  * 字段描述
  *
  * @param label 描述
  */
@scala.annotation.meta.field
case class Label(label: String) extends StaticAnnotation

/**
  * 必需项
  */
@scala.annotation.meta.field
case class Require() extends StaticAnnotation