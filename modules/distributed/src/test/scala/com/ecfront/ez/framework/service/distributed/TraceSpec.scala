package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.distributed.trace.DTrace

class TraceSpec extends MockStartupSpec {

  test("Trace测试") {

    // 定义一套流程，带分支
    DTrace.define("testFlow", List(
      "m1#s1",
      "m1#s2",
      List(
        List("m2#s3-1", "m2#s4-1"),
        List("m3#s3-2")
      ),
      "m1#s5"
    ))

    // 写日志，流程实例1，走第一个分支
    DTrace.log("jzy1","testFlow","m1","s1","step1")
    DTrace.log("jzy1","testFlow","m1","s2","step2")
    DTrace.log("jzy1","testFlow","m2","s3-1","step3")
    DTrace.log("jzy1","testFlow","m2","s4-1","step4")
    DTrace.log("jzy1","testFlow","m1","s5","step5")

    // 写日志，流程实例2，走第二个分支
    DTrace.log("jzy2","testFlow","m1","s1","step1")
    DTrace.log("jzy2","testFlow","m1","s2","step2")
    DTrace.log("jzy2","testFlow","m3","s3-2","step3")
    DTrace.log("jzy2","testFlow","m1","s5","step4")

    // 写日志，流程实例3，有未经过的节点
    DTrace.log("jzy3","testFlow","m1","s1","step1")
    DTrace.log("jzy3","testFlow","m3","s3-2","step3")
    DTrace.log("jzy3","testFlow","m1","s5","step4")

    // 写日志，流程实例4，有未经过的节点
    DTrace.log("jzy4","testFlow","m1","s1","step1")
    DTrace.log("jzy4","testFlow","m1","s2","step2")
    DTrace.log("jzy4","testFlow","m2","s3-1","step3")
    DTrace.log("jzy4","testFlow","m1","s5","step5")
  }

}





