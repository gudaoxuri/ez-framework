package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.core.test.BasicSpec
import com.ecfront.ez.framework.service.auth.helper.CaptchaHelper


class CaptchaSpec extends BasicSpec {

  test("captcha test") {
    val file = CaptchaHelper.generate("1234")
    println(file)
  }


}
