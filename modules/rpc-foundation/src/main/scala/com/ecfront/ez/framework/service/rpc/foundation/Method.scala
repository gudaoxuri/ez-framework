package com.ecfront.ez.framework.service.rpc.foundation

/**
 * 请求动作（方法）
 */
object Method extends Enumeration {
  val GET = Value("GET").toString
  val POST = Value("POST").toString
  val PUT = Value("PUT").toString
  val DELETE = Value("DELETE").toString
}