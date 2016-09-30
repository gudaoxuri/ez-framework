package com.ecfront.ez.framework.core.rpc

/**
 * 请求动作（方法）
 */
object Method extends Enumeration {
  val GET = Value("GET").toString
  val POST = Value("POST").toString
  val PUT = Value("PUT").toString
  val DELETE = Value("DELETE").toString
  val WS = Value("WS").toString
  val PUB_SUB = Value("PUB_SUB").toString
  val REQ_RESP = Value("REQ_RESP").toString
  val ACK = Value("ACK").toString
}