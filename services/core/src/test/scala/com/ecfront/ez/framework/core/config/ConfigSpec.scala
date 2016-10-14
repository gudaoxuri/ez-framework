package com.ecfront.ez.framework.core.config

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.{BasicSpec, EZ, EZManager}

class ConfigSpec extends BasicSpec {

  test("config test") {
    val tmpPath = this.getClass.getResource("/").getPath + "com/ecfront/ez/framework/core/config/conf"
    EZManager.start(s"@test#m1#$tmpPath")
    assert(EZ.Info.app == "test"
      && EZ.Info.module == "m1"
      && EZ.Info.timezone == "Asia/Shanghai"
      && EZ.Info.config.ez.cache == Map("address" -> "127.0.0.1:6379")
      && EZ.Info.config.ez.rpc == Map("package" -> "com.xx")
      && EZ.Info.config.ez.perf.nonEmpty
      && EZ.Info.config.ez.services.size == 3
      && JsonHelper.toJson(EZ.Info.config.ez.services("test1")).get("field1").asText() == "1"
      && JsonHelper.toJson(EZ.Info.config.ez.services("test2")).get("field1").asText() == "1"
      && JsonHelper.toJson(EZ.Info.config.ez.services("test3.sub1")).get("field1").asText() == "1"
      && EZ.Info.config.args.get("global_a").asText() == "global.a"
      && EZ.Info.config.args.get("global_b").asText() == "global.c"
      && EZ.Info.config.args.get("app_c").asText() == "app.c"
    )
  }

}


