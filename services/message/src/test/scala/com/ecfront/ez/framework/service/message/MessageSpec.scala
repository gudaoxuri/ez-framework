package com.ecfront.ez.framework.service.message

import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.core.rpc.{OptInfo, RespHttpClientProcessor}
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.jdbc.Page
import com.ecfront.ez.framework.service.message.entity.{EZ_Message, EZ_Message_Log}
import com.ecfront.ez.framework.test.AuthStartupSpec

class MessageSpec extends AuthStartupSpec {

  val baseURL = "http://127.0.0.1:8080/ez/message"

  test("Message Test") {
    EZ_Message.deleteByCond("")
    EZ_Message_Log.deleteByCond("")

    val startTime = TimeHelper.sf.parse("20161003000000")
    val endTime = TimeHelper.sf.parse("20161104000000")

    val sysAdminCode = EZ_Account.getByLoginId("sysadmin", "").body.code
    // 公共消息
    MessageService.sendToPublic("cate1", "0", "msgPub", "", startTime, endTime)
    // 个人消息
    MessageService.sendToAccount(sysAdminCode, "cate1", "0", "msgSysAdmin1", "", startTime, endTime)
    // 个人消息（使用模板）
    MessageService.sendToAccount(sysAdminCode, "cate1", "0", "apply_success", Map(
      "customer.name" -> "管理员",
      "product.name" -> "产品A"
    ), startTime, endTime)
    // 个人消息，不在时间点
    MessageService.sendToAccount(sysAdminCode, "cate1", "0", "msgSysAdmin3", "", endTime, endTime)
    MessageService.sendToRole("@admin", "cate1", "0", "msgAdmin", "", startTime, endTime)

    val optInfo =
      RespHttpClientProcessor.post[OptInfo]("http://127.0.0.1:8080/public/ez/auth/login/", Map("id" -> "sysadmin", "password" -> "admin")).body
    val token = optInfo.token
    assert(
      RespHttpClientProcessor.get[Int](s"$baseURL/unRead/number/?__ez_token__=$token").body == 3)
    // 获取未读消息列表
    var unReadMessages = RespHttpClientProcessor.get[List[EZ_Message]](s"$baseURL/unRead/?__ez_token__=$token").body
    assert(unReadMessages.length == 3)
    // 获取已读消息列表
    var readMessages = RespHttpClientProcessor.get[Page[EZ_Message]](s"$baseURL/read/1/10/?__ez_token__=$token").body
    assert(readMessages.recordTotal == 0)
    // 标记已读1条消息
    RespHttpClientProcessor.get[Void](s"$baseURL/${unReadMessages.head.id}/markRead/?__ez_token__=$token")
    readMessages = RespHttpClientProcessor.get[Page[EZ_Message]](s"$baseURL/read/1/10/?__ez_token__=$token").body
    assert(readMessages.recordTotal == 1)
    // 获取未读消息列表并标记已读
    unReadMessages = RespHttpClientProcessor.get[List[EZ_Message]](s"$baseURL/unRead/?markRead=true&__ez_token__=$token").body
    assert(unReadMessages.length == 2)
    readMessages = RespHttpClientProcessor.get[Page[EZ_Message]](s"$baseURL/read/1/10/?__ez_token__=$token").body
    assert(readMessages.recordTotal == 3)
    unReadMessages = RespHttpClientProcessor.get[List[EZ_Message]](s"$baseURL/unRead/?markRead=true&__ez_token__=$token").body
    assert(unReadMessages.isEmpty)
  }

}

