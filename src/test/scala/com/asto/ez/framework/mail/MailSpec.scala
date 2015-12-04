package com.asto.ez.framework.mail

import com.asto.ez.framework.{BasicSpec, EZGlobal}
import io.vertx.ext.mail.{MailClient, MailConfig}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MailSpec extends BasicSpec {

  override def before2(): Any = {
    MailProcessor.init(MailClient.createShared(EZGlobal.vertx, new MailConfig(EZGlobal.ez_mail)))
  }

  test("Mail Test") {

    val sendResp = Await.result(MailProcessor.send("hi-sb@ecfront.com", "i@sunisle.org", "test 1", "<h1>h1</h1><br/>1\r\n2\r\n"), Duration.Inf)
    assert(sendResp)

  }
}


