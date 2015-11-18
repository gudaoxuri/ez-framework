package com.asto.ez.framework.rpc

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

/**
 * 标记使用HTTP通道，用于Auto Building特性
 * <p>
 * 如写在类中表明此类下所有RPC方法都支持HTTP通道，如写在某个方法中表示此方法支持HTTP通道
 */
class HTTP extends StaticAnnotation

/**
 * 标记使用Web Sockets通道，用于Auto Building特性
 * <p>
 * 如写在类中表明此类下所有RPC方法都支持Web Sockets通道，如写在某个方法中表示此方法支持Web Sockets通道
 */
class WEB_SOCKETS extends StaticAnnotation

/**
 * 标记使用Event Bus通道，用于Auto Building特性
 * <p>
 * 如写在类中表明此类下所有RPC方法都支持Event Bus通道，如写在某个方法中表示此方法支持Event Bus通道
 */
class EVENT_BUS extends StaticAnnotation