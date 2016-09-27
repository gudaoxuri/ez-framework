package com.ecfront.ez.framework.service.email

import javax.activation.FileDataSource
import javax.mail.Message.RecipientType

import com.ecfront.common.Resp
import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeUtility
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.hazlewood.connor.bottema.emailaddress.EmailAddressValidator
import org.simplejavamail.email.Email
import org.simplejavamail.mailer.Mailer
import org.simplejavamail.mailer.config.{ServerConfig, TransportStrategy}

/**
  * Email处理类
  */
object EmailProcessor extends LazyLogging {

  private var mailer: Mailer = _

  private var defaultSender: String = _
  private var defaultSendAddress: String = _

  /**
    * 初始化
    *
    * @return 结果
    */
  private[email] def init(host: String, port: Int, userName: String, password: String,
                          protocol: String, poolSize: Int = 10,
                          _defaultSender: String = "", _defaultSendAddress: String = ""): Resp[String] = {
    val ts = protocol match {
      case "ssl" => TransportStrategy.SMTP_SSL
      case "tls" => TransportStrategy.SMTP_TLS
      case _ => TransportStrategy.SMTP_PLAIN
    }
    mailer = new Mailer(new ServerConfig(host, port, userName, password), ts)
    if (poolSize != -1) {
      mailer.setThreadPoolSize(poolSize)
    }
    defaultSender = _defaultSender
    defaultSendAddress = _defaultSendAddress
    Resp.success("")
  }

  def validateAddress(email: String): Boolean = {
    EmailAddressValidator.isValid(email)
  }

  /**
    * 发送email
    *
    * @param toName    toName
    * @param toAddress toAddress
    * @param title     标题
    * @param content   正文
    * @return 发送结果
    */
  def send(toName: String, toAddress: String, title: String, content: String): Resp[Void] = {
    send(defaultSender, defaultSendAddress, List((toName, toAddress)), null, null, title, content, null, async = false)
  }

  /**
    * 发送email
    *
    * @param toName      toName
    * @param toAddress   toAddress
    * @param title       标题
    * @param content     正文
    * @param attachments 附件，格式：Name - Path
    * @return 发送结果
    */
  def send(toName: String, toAddress: String, title: String, content: String, attachments: List[(String, String)]): Resp[Void] = {
    send(defaultSender, defaultSendAddress, List((toName, toAddress)), null, null, title, content, attachments, async = false)
  }

  /**
    * 发送email
    *
    * @param sender      sender
    * @param sendAddress sendAddress
    * @param to          to
    * @param cc          cc
    * @param bcc         bcc
    * @param title       标题
    * @param content     正文
    * @param attachments 附件，格式：Name - Path
    * @param async       async
    * @return 发送结果
    */
  def send(sender: String, sendAddress: String, to: List[(String, String)], cc: List[(String, String)], bcc: List[(String, String)],
           title: String, content: String, attachments: List[(String, String)], async: Boolean): Resp[Void] = {
    val email = new Email()
    try {
      email.setFromAddress(sender, sendAddress)
      if (to != null) {
        to.foreach {
          addr =>
            email.addRecipient(addr._1, addr._2, RecipientType.TO)
        }
      }
      if (cc != null) {
        cc.foreach {
          addr =>
            email.addRecipient(addr._1, addr._2, RecipientType.CC)
        }
      }
      if (bcc != null) {
        bcc.foreach {
          addr =>
            email.addRecipient(addr._1, addr._2, RecipientType.BCC)
        }
      }
      email.setSubject(title)
      email.setText(content)
      if (attachments != null) {
        attachments.foreach {
          attach =>
            email.addAttachment(MimeUtility.encodeText(attach._1), new FileDataSource(attach._2))
        }
      }
      logger.trace(s"Send mail [$title] to ${to.map(_._2).mkString(";")}")
      if (!async) {
        mailer.sendMail(email)
      } else {
        mailer.sendMail(email, true)
      }
      Resp.success(null)
    } catch {
      case e: Throwable =>
        logger.error(s"Send mail error:${e.getMessage}", e)
        Resp.serverError(s"Send mail error:${e.getMessage}")
    }


  }

}
