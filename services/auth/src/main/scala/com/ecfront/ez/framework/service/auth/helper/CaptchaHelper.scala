package com.ecfront.ez.framework.service.auth.helper

import java.io.{File, FileOutputStream}

import com.github.cage.GCage
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 验证码生成
  */
object CaptchaHelper extends LazyLogging {

  def generate(text: String): File = {
    val temp = File.createTempFile("ez_captcha_", ".jpg")
    val os = new FileOutputStream(temp)
    try {
      temp.deleteOnExit()
      new GCage().draw(text, os)
      temp
    } catch {
      case e: Throwable =>
        logger.error("Generate captche error.", e)
        null
    } finally {
      os.close()
    }
  }
}
