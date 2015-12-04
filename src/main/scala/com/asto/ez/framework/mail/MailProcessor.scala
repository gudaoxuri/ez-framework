package com.asto.ez.framework.mail

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.buffer.Buffer
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.mail.{MailAttachment, MailClient, MailMessage, MailResult}

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}

object MailProcessor extends LazyLogging {

  private var mailClient: MailClient = _

  def init(_mailClient: MailClient): Unit = {
    mailClient = _mailClient
  }

  def send(from: String, to: String, title: String, content: String): Future[Resp[Void]] = {
    send(from, List(to), null, null, title, content, List())
  }

  def send(from: String, to: List[String], title: String, content: String): Future[Resp[Void]] = {
    send(from, to, null, null, title, content, List())
  }

  def send(from: String, to: List[String], cc: List[String], bcc: List[String], title: String, content: String, attachments: List[(String, String, Buffer)]): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (from == null) {
      p.success(Resp.badRequest("FROM not empty."))
    } else if (to == null || to.isEmpty) {
      p.success(Resp.badRequest("TO not empty."))
    } else if (title == null || title.trim.isEmpty) {
      p.success(Resp.badRequest("TITLE not empty."))
    } else {
      val message = new MailMessage()
      message.setFrom(from)
      message.setTo(to)
      if (cc != null) message.setCc(cc)
      if (bcc != null) message.setBcc(bcc)
      message.setSubject(title)
      message.setHtml(content)
      if (attachments != null)
        attachments.foreach {
          attach =>
            val attachment = new MailAttachment()
            attachment.setName(attach._1)
            attachment.setContentType(attach._2)
            attachment.setData(attach._3)
            message.setAttachment(attachment)
        }
      mailClient.sendMail(message, new Handler[AsyncResult[MailResult]] {
        override def handle(event: AsyncResult[MailResult]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(null))
          } else {
            logger.error(s"Send mail [$title] error : ${event.cause().getMessage}", event.cause())
            p.success(Resp.serverUnavailable(s"Send mail [$title] error : ${event.cause().getMessage}"))
          }
        }
      })
    }
    p.future
  }

}
