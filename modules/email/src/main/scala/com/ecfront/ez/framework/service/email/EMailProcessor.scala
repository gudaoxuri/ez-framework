package com.ecfront.ez.framework.service.email

import java.util.concurrent.atomic.AtomicInteger

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.buffer.Buffer
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.mail._

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * Email处理类
  */
object EmailProcessor extends LazyLogging {

  private var mailClient: MailClient = _
  private var mailConfig: MailConfig = _
  // 最大连接数
  private var maxPoolSize: Int = _
  // 当前发送错误次数
  private val currentErrorCounter: AtomicInteger = new AtomicInteger(0)

  /**
    * 初始化
    *
    * @param config 配置项
    * @return 结果
    */
  def init(config: MailConfig): Resp[String] = {
    mailConfig = config
    maxPoolSize = mailConfig.getMaxPoolSize
    currentErrorCounter.set(0)
    init(MailClient.createShared(EZContext.vertx, mailConfig))
    Resp.success("")
  }

  /**
    * 初始化
    *
    * @param _mailClient email client实例
    */
  def init(_mailClient: MailClient): Unit = {
    mailClient = _mailClient
  }

  /**
    * 发送email
    *
    * @param to      to
    * @param title   标题
    * @param content 正文
    * @return 发送结果
    */
  def send(to: String, title: String, content: String): Resp[Void] = {
    send(mailConfig.getUsername, List(to), null, null, title, content, List())
  }

  /**
    * 发送email
    *
    * @param to      to
    * @param title   标题
    * @param content 正文
    * @return 发送结果
    */
  def send(to: List[String], title: String, content: String): Resp[Void] = {
    send(mailConfig.getUsername, to, null, null, title, content, List())
  }

  /**
    * 发送email
    *
    * @param from    from
    * @param to      to
    * @param title   标题
    * @param content 正文
    * @return 发送结果
    */
  def send(from: String, to: String, title: String, content: String): Resp[Void] = {
    send(from, List(to), null, null, title, content, List())
  }

  /**
    * 发送email
    *
    * @param from    from
    * @param to      to
    * @param title   标题
    * @param content 正文
    * @return 发送结果
    */
  def send(from: String, to: List[String], title: String, content: String): Resp[Void] = {
    send(from, to, null, null, title, content, List())
  }

  /**
    * 发送email
    *
    * @param from        from
    * @param to          to
    * @param cc          cc
    * @param bcc         bcc
    * @param title       标题
    * @param content     正文
    * @param attachments 附件
    * @return 发送结果
    */
  def send(from: String, to: List[String], cc: List[String], bcc: List[String],
           title: String, content: String, attachments: List[(String, String, Buffer)]): Resp[Void] = {
    Await.result(Async.send(from, to, cc, bcc, title, content, attachments), Duration.Inf)
  }

  object Async {

    /**
      * 发送email
      *
      * @param to      to
      * @param title   标题
      * @param content 正文
      * @return 发送结果
      */
    def send(to: String, title: String, content: String): Future[Resp[Void]] = {
      send(mailConfig.getUsername, List(to), null, null, title, content, List())
    }

    /**
      * 发送email
      *
      * @param to      to
      * @param title   标题
      * @param content 正文
      * @return 发送结果
      */
    def send(to: List[String], title: String, content: String): Future[Resp[Void]] = {
      send(mailConfig.getUsername, to, null, null, title, content, List())
    }

    /**
      * 发送email
      *
      * @param from    from
      * @param to      to
      * @param title   标题
      * @param content 正文
      * @return 发送结果
      */
    def send(from: String, to: String, title: String, content: String): Future[Resp[Void]] = {
      send(from, List(to), null, null, title, content, List())
    }

    /**
      * 发送email
      *
      * @param from    from
      * @param to      to
      * @param title   标题
      * @param content 正文
      * @return 发送结果
      */
    def send(from: String, to: List[String], title: String, content: String): Future[Resp[Void]] = {
      send(from, to, null, null, title, content, List())
    }

    /**
      * 发送email
      *
      * @param from        from
      * @param to          to
      * @param cc          cc
      * @param bcc         bcc
      * @param title       标题
      * @param content     正文
      * @param attachments 附件
      * @return 发送结果
      */
    def send(from: String, to: List[String], cc: List[String], bcc: List[String],
             title: String, content: String, attachments: List[(String, String, Buffer)]): Future[Resp[Void]] = {
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
        if (attachments != null) {
          attachments.foreach {
            attach =>
              val attachment = new MailAttachment()
              attachment.setName(attach._1)
              attachment.setContentType(attach._2)
              attachment.setData(attach._3)
              message.setAttachment(attachment)
          }
        }
        logger.trace(s"Send mail [$title]")
        mailClient.sendMail(message, new Handler[AsyncResult[MailResult]] {
          override def handle(event: AsyncResult[MailResult]): Unit = {
            if (event.succeeded()) {
              p.success(Resp.success(null))
            } else {
              if (currentErrorCounter.incrementAndGet() == maxPoolSize) {
                // 解决连接错误时不会释放连接池的bug
                logger.debug(s"Send mail error times equals max pool size , reinitialize mail client.")
                mailClient.close()
                init(mailConfig)
              }
              logger.error(s"Send mail [$title] error : ${event.cause().getMessage}", event.cause())
              p.success(Resp.serverUnavailable(s"Send mail [$title] error : ${event.cause().getMessage}"))
            }
          }
        })
      }
      p.future
    }

  }

}
