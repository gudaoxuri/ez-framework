package com.ecfront.ez.framework.service.auth.helper

import java.io.{File, FileOutputStream}

import com.ecfront.ez.framework.core.logger.Logging
import com.github.cage.GCage

/**
  * 验证码生成
  */
object CaptchaHelper extends Logging {

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
