package com.ecfront.ez.framework.service.weixin.api

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.rpc.foundation.{GET, POST, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.weixin.BaseProcessor
import com.ecfront.ez.framework.service.weixin.vo.UserVO

import scala.collection.mutable.ArrayBuffer

@RPC("/weixin/user/")
@HTTP
object UserProcessor extends BaseProcessor {

  private val MAX_FETCHED_USER_INFO_EACH_TIME: Int = 100

  @GET("ids/")
  def rpcFindAllIds(parameter: Map[String, String], context: EZAuthContext): Resp[List[String]] = {
    Resp.success(findAllIds())
  }

  def findAllIds(): List[String] = {
    val allUserIds = ArrayBuffer[String]()
    val result = getDataByTokenUrl("user/get")
    val total = result("total").asInstanceOf[Int]
    val count = result("count").asInstanceOf[Int]
    allUserIds ++= result("data").asInstanceOf[Map[String, String]]("openid").asInstanceOf[List[String]]
    if (total > count) {
      val nextOpenId = result("next_openid").asInstanceOf[String]
      findNextIds(allUserIds, count, nextOpenId)
    }
    allUserIds.toList
  }

  private def findNextIds(allUserIds: ArrayBuffer[String], loadedCount: Long, nextOpenId: String): Unit = {
    val result = getDataByTokenUrl("user/get?next_openid=" + nextOpenId)
    val total = result("total").asInstanceOf[Long]
    val count = result("count").asInstanceOf[Long]
    allUserIds ++= result("data").asInstanceOf[Map[String, String]]("openid").asInstanceOf[List[String]]
    if (total > count + loadedCount) {
      val nextOpenId = result("next_openid").asInstanceOf[String]
      findNextIds(allUserIds, count + loadedCount, nextOpenId)
    }
  }

  @GET(":openId/")
  def rpcGetUserInfo(parameter: Map[String, String], context: EZAuthContext): Resp[UserVO] = {
    Resp.success(getUserInfo(parameter("openId")))
  }

  def getUserInfo(openId: String): UserVO = {
    val result = getDataByTokenUrl(s"user/info?openid=$openId&lang=zh_CN")
    JsonHelper.toObject[UserVO](result)
  }

  @POST("info/")
  def rpcFindUserInfo(parameter: Map[String, String], body: String, context: EZAuthContext): Resp[List[UserVO]] = {
    Resp.success(findUserInfo(body.split(",").toList))
  }

  def findUserInfo(openIds: List[String]): List[UserVO] = {
    val request = openIds.map {
      id =>
        Map("openid" -> id)
    }
    request.grouped(MAX_FETCHED_USER_INFO_EACH_TIME).flatMap {
      ids =>
        val currentRequest = Map(
          "user_list" -> ids
        )
        val result = getDataByTokenUrl(s"user/info/batchget", currentRequest)
        JsonHelper.toObject[List[UserVO]](result("user_info_list"))
    }.toList
  }

}
