package com.ecfront.ez.framework.core.dist

import java.util.concurrent.CountDownLatch
import java.util.{Timer, TimerTask}

import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.test.MockStartupSpec

import scala.beans.BeanProperty


class MapSpec extends MockStartupSpec {

  test("Map Test") {

    val mapObj = EZ.dist.map[TestMapObj]("test_obj_map")
    mapObj.clear()
    val obj=new TestMapObj
    obj.a="测试"
    assert(mapObj.put("a",obj).get("a").a=="测试")


    val map = EZ.dist.map[Long]("test_map")
    map.clear()

    val timer = new Timer()
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        map.put("a", System.currentTimeMillis())
      }
    }, 0, 1000)
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        map.foreach({
          (k, v) =>
            println(">>a:" + v)
        })
      }
    }, 0, 10000)
    new CountDownLatch(1).await()
  }

}

class TestMapObj extends Serializable{
  @BeanProperty
  var a:String=_
}





