package com.ecfront.ez.framework.service.rpc.foundation

import scala.annotation.StaticAnnotation

/**
 * 标记需要注册RPC的类，用于Auto Building特性
 * @param baseUri 此类的根路径
 */
case class RPC(baseUri: String) extends StaticAnnotation

/**
 * 注册用于接收Get请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class GET(uri: String) extends StaticAnnotation

/**
 * 注册用于接收Post请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class POST(uri: String) extends StaticAnnotation

/**
 * 注册用于接收Put请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class PUT(uri: String) extends StaticAnnotation

/**
 * 注册用于接收Delete请求，用于Auto Building特性
 * @param uri 请求路径（baseUri+uri）
 */
case class DELETE(uri: String) extends StaticAnnotation