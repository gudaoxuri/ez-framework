package com.ecfront.ez.framework

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.i18n.I18NProcessor
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.core.test.MockStartupSpec

class I18NSpec extends MockStartupSpec {

  test("I18N test") {

    assert(Resp.badRequest("Code Not found").message == "Code Not found")
    assert(Resp.badRequest("[aadfs] Not Found").message == "[aadfs] Not Found")
    I18NProcessor.setLanguage("zh-CN")
    assert(Resp.badRequest("Code Not found").message == "编码不能为空")
    assert(Resp.badRequest("[aadfs] Not Found").message == "[aadfs] 不能为空")
    assert(Resp.badRequest("[sss,3re_w,不] must be unique").message == "[sss,3re_w,不] 不能为空")
    // 带tab
    assert(Resp.badRequest("[aadfs]	Not Found").message == "[aadfs] 不能为空")

    assert("Code Not found".x == "编码不能为空")

  }

}


